package kr.hhplus.be.server.common.exception;

public class FailedPaymentException extends RuntimeException {
    public FailedPaymentException(String message) {
        super(message);
    }
}
