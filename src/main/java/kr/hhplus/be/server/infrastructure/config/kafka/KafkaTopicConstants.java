package kr.hhplus.be.server.infrastructure.config.kafka;

/**
 * Kafka 토픽 이름들을 중앙에서 관리하는 상수 클래스
 */
public final class KafkaTopicConstants {
    
    private KafkaTopicConstants() {
    }
    
    /**
     * 주문 완료 이벤트 토픽
     */
    public static final String ORDER_COMPLETED = "order-completed";
    
    /**
     * 쿠폰 발급 요청 토픽
     */
    public static final String COUPON_ISSUE_REQUEST = "coupon-issue-request";
    
    /**
     * 쿠폰 발급 결과 토픽
     */
    public static final String COUPON_ISSUE_RESULT = "coupon-issue-result";
} 