package kr.hhplus.be.server.common.exception;

public class NotExistPaymentInfoException extends RuntimeException {
    public NotExistPaymentInfoException(String message) {
        super(message);
    }
}
