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
import static kr.hhplus.be.server.common.exception.ErrorCode.OUT_OF_STOCK_COUPON;

@SpringBootTest
@ActiveProfiles("test")
class CouponConcurrencyTest {

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
    void 동시에_여러_사용자가_쿠폰을_발급받을_때_재고가_정확히_차감되어야_한다() throws InterruptedException {
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
                    if (e.getErrorCode() == OUT_OF_STOCK_COUPON) {
                        failCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();

        // then
        Coupon updatedCoupon = couponRepository.findById(coupon.getId()).orElseThrow();
        assertThat(updatedCoupon.getStock()).isEqualTo(0);
        assertThat(successCount.get()).isEqualTo(STOCK);
        assertThat(failCount.get()).isEqualTo(THREAD_COUNT - STOCK);
        assertThat(userCouponRepository.findAll().size()).isEqualTo(STOCK);
    }
}
