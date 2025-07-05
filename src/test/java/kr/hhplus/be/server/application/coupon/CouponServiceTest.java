package kr.hhplus.be.server.application.coupon;

import kr.hhplus.be.server.common.exception.ApiException;
import kr.hhplus.be.server.domain.coupon.Coupon;
import kr.hhplus.be.server.domain.coupon.DiscountType;
import kr.hhplus.be.server.domain.coupon.UserCoupon;
import kr.hhplus.be.server.domain.coupon.UserCouponRepository;
import kr.hhplus.be.server.domain.coupon.CouponRepository;
import kr.hhplus.be.server.interfaces.web.coupon.dto.response.CouponListResponse;
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
import static org.mockito.BDDMockito.*;
import static kr.hhplus.be.server.common.exception.ErrorCode.*;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @Mock
    private UserCouponRepository userCouponRepository;

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private CouponRedisService couponRedisService;

    @Mock
    private CouponEventService couponEventService;

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
        given(couponRedisService.getCouponLimit(couponId)).willReturn(0L);
        given(couponRedisService.tryIssueCoupon(couponId, userId)).willReturn(true);
        given(userCouponRepository.save(any(UserCoupon.class))).willReturn(userCoupon);

        // when
        UserCoupon issuedCoupon = couponService.issueCoupon(userId, couponId);

        // then
        assertThat(issuedCoupon).isNotNull();
        assertThat(issuedCoupon.getUserId()).isEqualTo(userId);
        assertThat(issuedCoupon.getCouponId()).isEqualTo(couponId);
        assertThat(issuedCoupon.isUsed()).isFalse();
        
        then(couponRepository).should(times(1)).findById(couponId);
        then(couponRedisService).should(times(1)).setCouponLimit(couponId, coupon.getStock());
        then(couponRedisService).should(times(1)).tryIssueCoupon(couponId, userId);
        then(userCouponRepository).should(times(1)).save(any(UserCoupon.class));
        then(couponEventService).should(times(1)).publishCouponIssuedEvent(couponId, userId, userCoupon.getId());
    }

    @Test
    void 쿠폰_발급_실패_시_예외가_발생한다() {
        // given
        given(couponRepository.findById(couponId)).willReturn(Optional.of(coupon));
        given(couponRedisService.getCouponLimit(couponId)).willReturn(0L);
        given(couponRedisService.tryIssueCoupon(couponId, userId)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> couponService.issueCoupon(userId, couponId))
                .isInstanceOf(ApiException.class)
                .hasMessage(COUPON_ISSUANCE_FAILED.getMessage());

        then(couponRepository).should(times(1)).findById(couponId);
        then(couponRedisService).should(times(1)).setCouponLimit(couponId, coupon.getStock());
        then(couponRedisService).should(times(1)).tryIssueCoupon(couponId, userId);
        then(userCouponRepository).should(never()).save(any(UserCoupon.class));
    }

    @Test
    void 존재하지_않는_쿠폰_발급_요청_시_예외가_발생한다() {
        // given
        given(couponRepository.findById(couponId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> couponService.issueCoupon(userId, couponId))
                .isInstanceOf(ApiException.class)
                .hasMessage(COUPON_NOT_FOUND.getMessage());

        then(couponRepository).should(times(1)).findById(couponId);
        then(couponRedisService).should(never()).tryIssueCoupon(any(), any());
        then(userCouponRepository).should(never()).save(any(UserCoupon.class));
    }

    @Test
    void 사용자_쿠폰_목록_조회에_성공한다() {
        // given
        List<UserCoupon> userCoupons = Arrays.asList(userCoupon);
        given(userCouponRepository.findUnusedByUserId(userId)).willReturn(userCoupons);
        given(couponRepository.findById(couponId)).willReturn(Optional.of(coupon));

        // when
        CouponListResponse response = couponService.getUserCoupons(userId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getCoupons()).hasSize(1);
        assertThat(response.getCoupons().get(0).getTitle()).isEqualTo("신규 가입 쿠폰");
    }

    @Test
    void 사용자_쿠폰_목록이_비어있을_때_빈_리스트를_반환한다() {
        // given
        given(userCouponRepository.findUnusedByUserId(userId)).willReturn(Collections.emptyList());

        // when
        CouponListResponse response = couponService.getUserCoupons(userId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getCoupons()).isEmpty();
    }

    @Test
    void 할인_가격_계산에_성공한다() {
        // given
        Long userCouponId = 1L;
        Long totalPrice = 50000L;
        given(userCouponRepository.findById(userCouponId)).willReturn(Optional.of(userCoupon));
        given(couponRepository.findById(couponId)).willReturn(Optional.of(coupon));

        // when
        Long discountedPrice = couponService.calculateDiscountPrice(userCouponId, totalPrice);

        // then
        assertThat(discountedPrice).isEqualTo(40000L); // 50000 - 10000
    }

    @Test
    void 쿠폰_발급_순위_조회에_성공한다() {
        // given
        Long expectedRank = 5L;
        given(couponRedisService.getIssueRank(couponId, userId)).willReturn(expectedRank);

        // when
        Long actualRank = couponService.getIssueRank(couponId, userId);

        // then
        assertThat(actualRank).isEqualTo(expectedRank);
        then(couponRedisService).should(times(1)).getIssueRank(couponId, userId);
    }

    @Test
    void 쿠폰_발급_완료_수_조회_성공한다() {
        // given
        Long expectedCount = 50L;
        given(couponRedisService.getIssuedCount(couponId)).willReturn(expectedCount);

        // when
        Long actualCount = couponService.getIssuedCount(couponId);

        // then
        assertThat(actualCount).isEqualTo(expectedCount);
        then(couponRedisService).should(times(1)).getIssuedCount(couponId);
    }
} 