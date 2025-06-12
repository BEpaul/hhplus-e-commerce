package kr.hhplus.be.server.application.coupon;

import kr.hhplus.be.server.common.exception.OutOfStockCouponException;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class CouponConcurrencyTest {

    @Autowired
    private CouponService couponService;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private UserCouponRepository userCouponRepository;

    private Coupon coupon;

    @BeforeEach
    void setUp() {
        coupon = Coupon.builder()
                .discountValue(1000L)
                .discountType(DiscountType.AMOUNT)
                .title("테스트 쿠폰")
                .stock(100L)
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusDays(30))
                .build();
        couponRepository.save(coupon);
    }

    @Test
    void 여러_사용자가_동시에_쿠폰을_발급받을_때_재고가_정확히_감소해야_한다() throws InterruptedException {
        // given
        int numberOfUsers = 100;
        int numberOfThreads = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfUsers);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when
        List<Long> userIds = new ArrayList<>();
        for (int i = 0; i < numberOfUsers; i++) {
            final Long userId = (long) i;
            userIds.add(userId);
            executorService.execute(() -> {
                try {
                    couponService.issueCoupon(userId, coupon.getId());
                    successCount.incrementAndGet();
                } catch (OutOfStockCouponException e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();

        // then
        Coupon updatedCoupon = couponRepository.findById(coupon.getId()).orElseThrow();
        assertThat(updatedCoupon.getStock()).isEqualTo(0L);
        assertThat(successCount.get()).isEqualTo(100);
        assertThat(failCount.get()).isEqualTo(0);
        assertThat(userCouponRepository.findAll().size()).isEqualTo(100);
    }

    @Test
    void 재고가_부족한_상황에서_동시에_쿠폰을_발급받으려고_할_때_적절히_처리되어야_한다() throws InterruptedException {
        // given
        int numberOfUsers = 200; // 재고보다 많은 사용자
        int numberOfThreads = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfUsers);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when
        List<Long> userIds = new ArrayList<>();
        for (int i = 0; i < numberOfUsers; i++) {
            final Long userId = (long) i;
            userIds.add(userId);
            executorService.execute(() -> {
                try {
                    couponService.issueCoupon(userId, coupon.getId());
                    successCount.incrementAndGet();
                } catch (OutOfStockCouponException e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();

        // then
        Coupon updatedCoupon = couponRepository.findById(coupon.getId()).orElseThrow();
        assertThat(updatedCoupon.getStock()).isEqualTo(0L);
        assertThat(successCount.get()).isEqualTo(100); // 재고만큼만 성공
        assertThat(failCount.get()).isEqualTo(100); // 나머지는 실패
        assertThat(userCouponRepository.findAll().size()).isEqualTo(100);
    }
}
