package kr.hhplus.be.server.application.coupon;

import kr.hhplus.be.server.common.exception.ApiException;
import kr.hhplus.be.server.domain.coupon.Coupon;
import kr.hhplus.be.server.domain.coupon.DiscountType;
import kr.hhplus.be.server.domain.coupon.UserCoupon;
import kr.hhplus.be.server.domain.coupon.UserCouponRepository;
import kr.hhplus.be.server.domain.coupon.CouponRepository;
import kr.hhplus.be.server.infrastructure.config.redis.DistributedLockService;
import kr.hhplus.be.server.interfaces.web.coupon.dto.response.CouponListResponse;
import kr.hhplus.be.server.interfaces.web.coupon.dto.response.CouponResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.lenient;
import static kr.hhplus.be.server.common.exception.ErrorCode.*;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @Mock
    private UserCouponRepository userCouponRepository;

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private DistributedLockService distributedLockService;

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
            
        // 분산락 모킹 설정 - 락을 성공적으로 획득하고 작업을 실행하도록 설정
        lenient().when(distributedLockService.executePointLock(any(), any())).thenAnswer(invocation -> {
            return invocation.getArgument(1, java.util.function.Supplier.class).get();
        });
        lenient().when(distributedLockService.executeWithLock(any(String.class), any(Long.class), any(Long.class), any(java.util.function.Supplier.class))).thenAnswer(invocation -> {
            return invocation.getArgument(3, java.util.function.Supplier.class).get();
        });
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
        then(distributedLockService).should().executePointLock(eq(userId), any());
    }

    @Test
    void 사용자가_가지고_있지_않은_쿠폰_사용_시_예외가_발생한다() {
        // given
        Long userCouponId = 999L;
        given(userCouponRepository.findById(userCouponId))
            .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> couponService.useCoupon(userCouponId))
                .isInstanceOf(ApiException.class)
                .hasMessage(NOT_OWNED_USER_COUPON.getMessage());

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
            .isInstanceOf(ApiException.class)
            .hasMessage(ALREADY_USED_COUPON.getMessage());

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
        then(distributedLockService).should().executeWithLock(eq("coupon:issue:" + couponId), eq(10L), eq(30L), any(java.util.function.Supplier.class));
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
                .isInstanceOf(ApiException.class)
                .hasMessage(OUT_OF_STOCK_COUPON.getMessage());

        then(couponRepository).should(times(1)).findById(couponId);
        then(userCouponRepository).should(never()).save(any(UserCoupon.class));
        then(distributedLockService).should().executeWithLock(eq("coupon:issue:" + couponId), eq(10L), eq(30L), any(java.util.function.Supplier.class));
    }

    @Test
    void 사용자의_쿠폰_목록이_정상적으로_조회된다() {
        // given
        Long secondCouponId = 201L;
        UserCoupon secondUserCoupon = UserCoupon.builder()
            .id(2L)
            .userId(userId)
            .couponId(secondCouponId)
            .isUsed(false)
            .expiredAt(LocalDateTime.now().plusDays(14))
            .build();

        Coupon secondCoupon = Coupon.builder()
            .id(secondCouponId)
            .discountValue(20L)
            .discountType(DiscountType.PERCENT)
            .title("여름 맞이 할인 쿠폰")
            .stock(50L)
            .startDate(LocalDateTime.now())
            .endDate(LocalDateTime.now().plusDays(60))
            .build();

        List<UserCoupon> userCoupons = Arrays.asList(userCoupon, secondUserCoupon);
        given(userCouponRepository.findUnusedByUserId(userId)).willReturn(userCoupons);
        given(couponRepository.findById(couponId)).willReturn(Optional.of(coupon));
        given(couponRepository.findById(secondCouponId)).willReturn(Optional.of(secondCoupon));

        // when
        CouponListResponse response = couponService.getUserCoupons(userId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getCoupons()).hasSize(2);
        
        List<CouponResponse> coupons = response.getCoupons();
        assertThat(coupons.get(0).getId()).isEqualTo(couponId);
        assertThat(coupons.get(0).getTitle()).isEqualTo("신규 가입 쿠폰");
        assertThat(coupons.get(0).getDiscountType()).isEqualTo(DiscountType.AMOUNT);
        assertThat(coupons.get(0).getDiscountValue()).isEqualTo(10000L);
        
        assertThat(coupons.get(1).getId()).isEqualTo(secondCouponId);
        assertThat(coupons.get(1).getTitle()).isEqualTo("여름 맞이 할인 쿠폰");
        assertThat(coupons.get(1).getDiscountType()).isEqualTo(DiscountType.PERCENT);
        assertThat(coupons.get(1).getDiscountValue()).isEqualTo(20L);

        then(userCouponRepository).should(times(1)).findUnusedByUserId(userId);
        then(couponRepository).should(times(1)).findById(couponId);
        then(couponRepository).should(times(1)).findById(secondCouponId);
    }

    @Test
    void 사용자의_사용_가능한_쿠폰_목록이_없는_경우_빈_배열이_반환된다() {
        // given
        given(userCouponRepository.findUnusedByUserId(userId)).willReturn(Collections.emptyList());

        // when
        CouponListResponse response = couponService.getUserCoupons(userId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getCoupons()).isEmpty();

        then(userCouponRepository).should(times(1)).findUnusedByUserId(userId);
        then(couponRepository).should(never()).findById(any());
    }

    @Test
    void 사용한_쿠폰은_조회되지_않는다() {
        // given
        UserCoupon usedCoupon = UserCoupon.builder()
            .id(1L)
            .userId(userId)
            .couponId(couponId)
            .isUsed(true)
            .expiredAt(LocalDateTime.now().plusDays(7))
            .build();

        given(userCouponRepository.findUnusedByUserId(userId)).willReturn(Collections.emptyList());

        // when
        CouponListResponse response = couponService.getUserCoupons(userId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getCoupons()).isEmpty();

        then(userCouponRepository).should(times(1)).findUnusedByUserId(userId);
        then(couponRepository).should(never()).findById(any());
    }
} 