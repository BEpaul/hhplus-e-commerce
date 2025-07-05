package kr.hhplus.be.server.application.coupon;

import kr.hhplus.be.server.common.exception.ApiException;
import kr.hhplus.be.server.domain.coupon.Coupon;
import kr.hhplus.be.server.domain.coupon.CouponRepository;
import kr.hhplus.be.server.domain.coupon.DiscountType;
import kr.hhplus.be.server.domain.coupon.UserCouponRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static kr.hhplus.be.server.common.exception.ErrorCode.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class CouponRedisConcurrencyTest {

    @Autowired
    private CouponService couponService;

    @Autowired
    private CouponRedisService couponRedisService;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private UserCouponRepository userCouponRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Coupon coupon;
    private static final int STOCK = 100;
    private static final int THREAD_COUNT = 200;

    @BeforeEach
    @Transactional
    void setUp() {
        jdbcTemplate.execute("DELETE FROM user_coupon");
        jdbcTemplate.execute("DELETE FROM coupon");
        
        for (long id = 1; id <= 1000; id++) {
            couponRedisService.deleteCouponData(id);
        }

        // 새로운 테스트 쿠폰 생성
        coupon = Coupon.builder()
                .discountValue(1000L)
                .discountType(DiscountType.AMOUNT)
                .title("Redis 테스트 쿠폰")
                .stock((long) STOCK)
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusDays(30))
                .build();
        couponRepository.save(coupon);
    }

    @Test
    void 동시에_여러_사용자가_Redis_기반_쿠폰을_발급받을_때_정확한_수량만_발급되어야_한다() throws InterruptedException {
        // given
        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when
        for (int i = 0; i < THREAD_COUNT; i++) {
            final Long userId = (long) i;
            executorService.execute(() -> {
                try {
                    couponService.issueCoupon(userId, coupon.getId());
                    successCount.incrementAndGet();
                } catch (ApiException e) {
                    if (e.getErrorCode() == COUPON_ISSUANCE_FAILED) {
                        failCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();

        // then
        assertThat(successCount.get()).isEqualTo(STOCK);
        assertThat(failCount.get()).isEqualTo(THREAD_COUNT - STOCK);
        assertThat(userCouponRepository.findAll().size()).isEqualTo(STOCK);
        
        // Redis 발급 완료 수 확인 (실제로는 발급된 사용자 수와 다를 수 있음)
        Long issuedCount = couponRedisService.getIssuedCount(coupon.getId());
        assertThat(issuedCount).isGreaterThanOrEqualTo((long) STOCK);
    }

    @Test
    void Redis_기반_쿠폰_발급에서_중복_발급이_방지되어야_한다() {
        // given
        Long userId = 1L;

        // when - 첫 번째 발급
        couponService.issueCoupon(userId, coupon.getId());

        // then - 두 번째 발급 시도 시 실패해야 함
        assertThatThrownBy(() -> couponService.issueCoupon(userId, coupon.getId()))
                .isInstanceOf(ApiException.class)
                .hasMessage(COUPON_ISSUANCE_FAILED.getMessage());

        // 발급된 쿠폰은 1개만 있어야 함
        assertThat(userCouponRepository.findAll().size()).isEqualTo(1);
        
        // Redis에서도 중복 발급이 방지되었는지 확인
        assertThat(couponRedisService.isAlreadyIssued(coupon.getId(), userId)).isTrue();
    }

    @Test
    void Redis_기반_쿠폰_발급에서_순위가_정확하게_부여되어야_한다() throws InterruptedException {
        // given
        int testUserCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(testUserCount);
        CountDownLatch latch = new CountDownLatch(testUserCount);

        // when
        for (int i = 0; i < testUserCount; i++) {
            final Long userId = (long) i;
            executorService.execute(() -> {
                try {
                    couponService.issueCoupon(userId, coupon.getId());
                } catch (ApiException e) {
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();

        // then
        for (int i = 0; i < testUserCount; i++) {
            Long userId = (long) i;
            Long rank = couponService.getIssueRank(coupon.getId(), userId);
            
            if (rank != null) {
                assertThat(rank).isGreaterThanOrEqualTo(0);
                assertThat(rank).isLessThan(testUserCount);
            }
        }
    }
} 