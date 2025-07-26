package kr.hhplus.be.server.common.exception;

/**
 * 재시도 가능한 예외를 나타내는 클래스
 * 일시적인 오류나 네트워크 문제 등으로 인해 발생하는 예외에 사용
 */
public class RetryableException extends RuntimeException {
    
    public RetryableException(String message) {
        super(message);
    }
    
    public RetryableException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public RetryableException(Throwable cause) {
        super(cause);
    }
} 