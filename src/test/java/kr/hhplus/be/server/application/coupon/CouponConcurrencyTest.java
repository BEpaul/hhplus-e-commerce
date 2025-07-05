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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static kr.hhplus.be.server.common.exception.ErrorCode.COUPON_ISSUANCE_FAILED;

@SpringBootTest
@ActiveProfiles("test")
class CouponConcurrencyTest {

    @Autowired
    private CouponService couponService;

    @Autowired
    private CouponRedisService couponRedisService;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private UserCouponRepository userCouponRepository;

    private Coupon coupon;
    private static final int STOCK = 100;
    private static final int THREAD_COUNT = 200;

    @BeforeEach
    @Transactional
    void setUp() {
        // 기존 데이터 정리
        if (coupon != null) {
            couponRedisService.deleteCouponData(coupon.getId());
        }
        
        // 기존 UserCoupon 데이터 정리 (테스트 격리를 위해)
        // 실제로는 테스트 환경에서만 사용해야 함
        
        coupon = Coupon.builder()
                .discountValue(1000L)
                .discountType(DiscountType.AMOUNT)
                .title("테스트 쿠폰")
                .stock((long) STOCK)
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusDays(30))
                .build();
        couponRepository.save(coupon);
    }

    @Test
    void 동시에_여러_사용자가_쿠폰을_발급받을_때_정확한_수량만_발급되어야_한다() throws InterruptedException {
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
        
        // Redis 기반 발급 완료 수 확인
        Long issuedCount = couponRedisService.getIssuedCount(coupon.getId());
        assertThat(issuedCount).isEqualTo((long) STOCK);
        
        // RDB UserCoupon 생성 확인 (비동기 처리 전이므로 즉시 확인)
        assertThat(userCouponRepository.findAll().size()).isEqualTo(STOCK);
        
        // RDB 쿠폰 재고는 비동기로 차감되므로 원본 값 유지 확인
        Coupon updatedCoupon = couponRepository.findById(coupon.getId()).orElseThrow();
        assertThat(updatedCoupon.getStock()).isEqualTo((long) STOCK);
    }
}
