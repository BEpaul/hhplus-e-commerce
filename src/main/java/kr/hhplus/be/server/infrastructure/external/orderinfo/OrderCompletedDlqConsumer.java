package kr.hhplus.be.server.infrastructure.external.orderinfo;

import kr.hhplus.be.server.domain.order.event.dto.OrderCompletedDlqEventDto;
import kr.hhplus.be.server.infrastructure.config.kafka.KafkaTopicConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OrderCompletedDlqConsumer {

    private final OrderCompletedRetryService orderCompletedRetryService;

    public OrderCompletedDlqConsumer(OrderCompletedRetryService orderCompletedRetryService) {
        this.orderCompletedRetryService = orderCompletedRetryService;
    }

    @KafkaListener(
            topics = KafkaTopicConstants.ORDER_COMPLETED_DLQ,
            containerFactory = "orderCompletedDlqKafkaListenerContainerFactory"
    )
    public void handleDlqMessage(
            @Payload OrderCompletedDlqEventDto dlqEvent,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {
        
        try {
            log.info("DLQ 메시지 수신 - 주문 ID: {}, 토픽: {}, 파티션: {}, 오프셋: {}, 재시도 횟수: {}", 
                    dlqEvent.getOriginalEvent().getOrderId(), topic, partition, offset, dlqEvent.getRetryCount());
            
            if (dlqEvent.canRetry()) {
                log.info("DLQ 메시지 재처리 시작 - 주문 ID: {}", 
                        dlqEvent.getOriginalEvent().getOrderId());
                
                orderCompletedRetryService.retryFromDlq(dlqEvent);
                
                log.info("DLQ 메시지 재처리 요청 완료 - 주문 ID: {}", 
                        dlqEvent.getOriginalEvent().getOrderId());
            } else {
                log.error("최대 재시도 횟수 초과로 재처리 중단 - 주문 ID: {}, 실패 원인: {}", 
                        dlqEvent.getOriginalEvent().getOrderId(), dlqEvent.getFailureReason());
                
                handleFinalFailure(dlqEvent);
            }
            
            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            log.error("DLQ 메시지 처리 중 예외 발생 - 주문 ID: {}", 
                    dlqEvent.getOriginalEvent().getOrderId(), e);
            
            // 예외 발생 시에도 ACK 처리 (무한 루프 방지)
            acknowledgment.acknowledge();
        }
    }

    private void handleFinalFailure(OrderCompletedDlqEventDto dlqEvent) {
        log.error("주문 완료 이벤트 최종 실패 - 주문 ID: {}, 실패 원인: {}, 실패 시간: {}", 
                dlqEvent.getOriginalEvent().getOrderId(),
                dlqEvent.getFailureReason(),
                dlqEvent.getFailedAt());
        
        // TODO: 관리자 알림 로직 추가
        // TODO: 실패 통계 수집 로직 추가
        // TODO: 데이터베이스에 실패 기록 저장 로직 추가
    }
} 