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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class CouponKafkaConcurrencyTest {

    @Autowired
    private CouponService couponService;

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
    void 동시에_여러_사용자가_쿠폰을_발급_요청할_때_기본_검증이_정상적으로_동작해야_한다() throws InterruptedException {
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
        // Kafka 기반 시스템에서는 기본 검증만 수행하고 즉시 응답하므로
        // 모든 요청이 성공적으로 처리되어야 함 (실제 발급은 비동기로 처리)
        assertThat(successCount.get()).isEqualTo(THREAD_COUNT);
        assertThat(failCount.get()).isEqualTo(0);
    }

    @Test
    void 재고_부족_시_예외가_발생해야_한다() {
        // given
        Coupon outOfStockCoupon = Coupon.builder()
                .discountValue(1000L)
                .discountType(DiscountType.AMOUNT)
                .title("재고 없는 쿠폰")
                .stock(0L) // 재고 없음
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusDays(30))
                .build();
        couponRepository.save(outOfStockCoupon);

        // when & then
        assertThatThrownBy(() -> couponService.issueCoupon(1L, outOfStockCoupon.getId()))
                .isInstanceOf(ApiException.class)
                .hasMessage("쿠폰 발급에 실패했습니다.");
    }
} 