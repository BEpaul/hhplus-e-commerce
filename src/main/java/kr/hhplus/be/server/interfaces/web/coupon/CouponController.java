package kr.hhplus.be.server.interfaces.web.coupon;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.hhplus.be.server.application.coupon.CouponService;
import kr.hhplus.be.server.common.response.ApiResponse;
import kr.hhplus.be.server.domain.coupon.UserCoupon;
import kr.hhplus.be.server.interfaces.web.coupon.dto.request.CouponIssueRequest;
import kr.hhplus.be.server.interfaces.web.coupon.dto.request.CouponIssueResponse;
import kr.hhplus.be.server.interfaces.web.coupon.dto.response.CouponIssueStatusResponse;
import kr.hhplus.be.server.interfaces.web.coupon.dto.response.CouponListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "쿠폰", description = "쿠폰 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class CouponController {

    private final CouponService couponService;

    @Operation(summary = "쿠폰 발급", description = "사용자에게 쿠폰을 발급합니다.")
    @PostMapping("/coupons")
    public ApiResponse<CouponIssueResponse> issueCoupon(@RequestBody CouponIssueRequest request) {
        UserCoupon userCoupon = couponService.issueCoupon(request.getUserId(), request.getCouponId());

        return ApiResponse.success(CouponIssueResponse.from(userCoupon.getId()), "쿠폰 발급 성공");
    }

    @Operation(summary = "쿠폰 발급 상태 조회", description = "사용자의 쿠폰 발급 순위와 상태를 조회합니다.")
    @GetMapping("/coupons/{couponId}/status")
    public ApiResponse<CouponIssueStatusResponse> getCouponIssueStatus(
            @PathVariable Long couponId,
            @RequestParam Long userId) {
        
        Long issueRank = couponService.getIssueRank(couponId, userId);
        Long issuedCount = couponService.getIssuedCount(couponId);
        
        CouponIssueStatusResponse response = CouponIssueStatusResponse.builder()
                .couponId(couponId)
                .userId(userId)
                .issueRank(issueRank)
                .issuedCount(issuedCount)
                .totalLimit(issuedCount) // 실제로는 쿠폰의 총 제한 수를 가져와야 함
                .isIssued(issueRank != null && issueRank >= 0)
                .build();

        return ApiResponse.success(response, "쿠폰 발급 상태 조회 성공");
    }

    @Operation(summary = "쿠폰 목록 조회", description = "사용자가 보유한 쿠폰 목록을 조회합니다.")
    @GetMapping("/users/{userId}/coupons")
    public ApiResponse<CouponListResponse> getUserCoupons(@PathVariable Long userId) {
        CouponListResponse response = couponService.getUserCoupons(userId);
        return ApiResponse.success(response, "쿠폰 목록 조회 성공");
    }
}
