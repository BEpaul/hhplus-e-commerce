package kr.hhplus.be.server.common.exception;

public class AlreadyAppliedCoupon extends RuntimeException {
    public AlreadyAppliedCoupon(String message) {
        super(message);
    }
}
