package kr.hhplus.be.server.interfaces.web.coupon;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.hhplus.be.server.application.coupon.CouponService;
import kr.hhplus.be.server.common.response.ApiResponse;
import kr.hhplus.be.server.domain.coupon.UserCoupon;
import kr.hhplus.be.server.interfaces.web.coupon.dto.request.CouponIssueRequest;
import kr.hhplus.be.server.interfaces.web.coupon.dto.request.CouponIssueResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "쿠폰", description = "쿠폰 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/coupons")
public class CouponController {

    private final CouponService couponService;

    @Operation(summary = "쿠폰 발급", description = "사용자에게 쿠폰을 발급합니다.")
    @PostMapping
    public ApiResponse<CouponIssueResponse> issueCoupon(@RequestBody CouponIssueRequest request) {
        UserCoupon userCoupon = couponService.issueCoupon(request.getUserId(), request.getCouponId());

        return ApiResponse.success(CouponIssueResponse.from(userCoupon.getId()), "쿠폰 발급 성공");
    }
}
