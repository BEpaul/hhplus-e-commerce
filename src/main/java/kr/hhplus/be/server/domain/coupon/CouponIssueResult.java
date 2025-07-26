package kr.hhplus.be.server.domain.coupon;

import kr.hhplus.be.server.interfaces.web.coupon.dto.event.CouponIssueRequestEventDto;
import kr.hhplus.be.server.interfaces.web.coupon.dto.event.CouponIssueResultEventDto;
import lombok.Getter;

@Getter
public class CouponIssueResult {
    private final boolean success;
    private final String errorMessage;
    private final Long userCouponId;
    private final Long remainingStock;
    private final CouponIssueRequestEventDto requestEvent;

    private CouponIssueResult(boolean success, String errorMessage, Long userCouponId,
                            Long remainingStock, CouponIssueRequestEventDto requestEvent) {
        this.success = success;
        this.errorMessage = errorMessage;
        this.userCouponId = userCouponId;
        this.remainingStock = remainingStock;
        this.requestEvent = requestEvent;
    }

    public static CouponIssueResult success(CouponIssueRequestEventDto requestEvent,
                                          Long userCouponId, Long remainingStock) {
        return new CouponIssueResult(true, null, userCouponId, remainingStock, requestEvent);
    }

    public static CouponIssueResult duplicateIssued(CouponIssueRequestEventDto requestEvent, String message) {
        return new CouponIssueResult(false, message, null, 0L, requestEvent);
    }

    public static CouponIssueResult outOfStock(CouponIssueRequestEventDto requestEvent, String message) {
        return new CouponIssueResult(false, message, null, 0L, requestEvent);
    }

    public static CouponIssueResult error(CouponIssueRequestEventDto requestEvent, String message) {
        return new CouponIssueResult(false, message, null, 0L, requestEvent);
    }

    public CouponIssueResultEventDto toEventDto() {
        if (success) {
            return CouponIssueResultEventDto.success(
                requestEvent.getRequestId(),
                requestEvent.getUserId(),
                requestEvent.getCouponId(),
                userCouponId
            );
        } else {
            return CouponIssueResultEventDto.failure(
                requestEvent.getRequestId(),
                requestEvent.getUserId(),
                requestEvent.getCouponId(),
                errorMessage
            );
        }
    }
}
