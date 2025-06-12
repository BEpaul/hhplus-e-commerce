package kr.hhplus.be.server.application.coupon;

import kr.hhplus.be.server.common.exception.AlreadyUsedCouponException;
import kr.hhplus.be.server.common.exception.NotOwnedUserCouponException;
import kr.hhplus.be.server.common.exception.OutOfStockCouponException;
import kr.hhplus.be.server.domain.coupon.Coupon;
import kr.hhplus.be.server.domain.coupon.DiscountType;
import kr.hhplus.be.server.domain.coupon.UserCoupon;
import kr.hhplus.be.server.domain.coupon.UserCouponRepository;
import kr.hhplus.be.server.domain.coupon.CouponRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @Mock
    private UserCouponRepository userCouponRepository;

    @Mock
    private CouponRepository couponRepository;

    @InjectMocks
    private CouponService couponService;

    private UserCoupon userCoupon;
    private Coupon coupon;
    private Long userId;
    private Long couponId;

    @BeforeEach
    void setUp() {
        userId = 100L;
        couponId = 200L;
        
        userCoupon = UserCoupon.builder()
            .id(1L)
            .userId(userId)
            .couponId(couponId)
            .isUsed(false)
            .expiredAt(LocalDateTime.now().plusDays(7))
            .build();
            
        coupon = Coupon.builder()
            .id(couponId)
            .discountValue(10000L)
            .discountType(DiscountType.AMOUNT)
            .title("신규 가입 쿠폰")
            .stock(100L)
            .startDate(LocalDateTime.now().minusDays(1))
            .endDate(LocalDateTime.now().plusDays(30))
            .build();
    }

    @Test
    void 쿠폰_사용에_성공한다() {
        // given
        Long userCouponId = 1L;
        given(userCouponRepository.findById(userCouponId))
            .willReturn(Optional.of(userCoupon));

        // when
        couponService.useCoupon(userCouponId);

        // then
        then(userCouponRepository).should(times(1)).findById(userCouponId);
        then(userCouponRepository).should(times(1)).save(any(UserCoupon.class));
    }

    @Test
    void 사용자가_가지고_있지_않은_쿠폰_사용_시_예외가_발생한다() {
        // given
        Long userCouponId = 999L;
        given(userCouponRepository.findById(userCouponId))
            .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> couponService.useCoupon(userCouponId))
                .isInstanceOf(NotOwnedUserCouponException.class);

        then(userCouponRepository).should(times(1)).findById(userCouponId);
        then(userCouponRepository).should(never()).save(any(UserCoupon.class));
    }

    @Test
    void 이미_사용된_쿠폰_사용_시_예외가_발생한다() {
        // given
        Long userCouponId = 1L;
        UserCoupon usedCoupon = UserCoupon.builder()
            .id(userCouponId)
            .userId(100L)
            .couponId(200L)
            .isUsed(true)
            .expiredAt(LocalDateTime.now().plusDays(7))
            .build();

        given(userCouponRepository.findById(userCouponId))
            .willReturn(Optional.of(usedCoupon));

        // when & then
        assertThatThrownBy(() -> couponService.useCoupon(userCouponId))
            .isInstanceOf(AlreadyUsedCouponException.class)
            .hasMessage("이미 사용된 쿠폰입니다.");

        then(userCouponRepository).should(times(1)).findById(userCouponId);
        then(userCouponRepository).should(never()).save(any(UserCoupon.class));
    }

    @Test
    void 쿠폰이_정상적으로_발급된다() {
        // given
        given(couponRepository.findById(couponId)).willReturn(Optional.of(coupon));
        given(userCouponRepository.save(any(UserCoupon.class))).willReturn(userCoupon);

        // when
        UserCoupon issuedCoupon = couponService.issueCoupon(userId, couponId);

        // then
        assertThat(issuedCoupon).isNotNull();
        assertThat(issuedCoupon.getUserId()).isEqualTo(userId);
        assertThat(issuedCoupon.getCouponId()).isEqualTo(couponId);
        assertThat(issuedCoupon.isUsed()).isFalse();
        
        then(couponRepository).should(times(1)).findById(couponId);
        then(userCouponRepository).should(times(1)).save(any(UserCoupon.class));
    }

    @Test
    void 쿠폰_잔여수량이_0이면_발급_요청_시_예외가_발생한다() {
        // given
        Coupon outOfStockCoupon = Coupon.builder()
            .id(couponId)
            .discountValue(10000L)
            .discountType(DiscountType.AMOUNT)
            .title("신규 가입 쿠폰")
            .stock(0L) // 재고가 0인 쿠폰
            .startDate(LocalDateTime.now().minusDays(1))
            .endDate(LocalDateTime.now().plusDays(30))
            .build();
            
        given(couponRepository.findById(couponId)).willReturn(Optional.of(outOfStockCoupon));

        // when & then
        assertThatThrownBy(() -> couponService.issueCoupon(userId, couponId))
                .isInstanceOf(OutOfStockCouponException.class);

        then(couponRepository).should(times(1)).findById(couponId);
        then(userCouponRepository).should(never()).save(any(UserCoupon.class));
    }
} 