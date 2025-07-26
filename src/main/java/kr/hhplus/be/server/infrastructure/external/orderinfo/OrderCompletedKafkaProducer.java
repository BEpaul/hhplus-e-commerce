package kr.hhplus.be.server.infrastructure.external.orderinfo;

import kr.hhplus.be.server.domain.order.event.dto.OrderCompletedEventDto;
import kr.hhplus.be.server.domain.order.event.dto.OrderCompletedDlqEventDto;
import kr.hhplus.be.server.infrastructure.config.kafka.KafkaTopicConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
public class OrderCompletedKafkaProducer {

    private final KafkaTemplate<String, OrderCompletedEventDto> kafkaTemplate;
    private final KafkaTemplate<String, OrderCompletedDlqEventDto> dlqKafkaTemplate;

    public OrderCompletedKafkaProducer(
            @Qualifier("orderCompletedKafkaTemplate") KafkaTemplate<String, OrderCompletedEventDto> kafkaTemplate,
            @Qualifier("orderCompletedDlqKafkaTemplate") KafkaTemplate<String, OrderCompletedDlqEventDto> dlqKafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        this.dlqKafkaTemplate = dlqKafkaTemplate;
    }

    /**
     * 주문 완료 이벤트를 Kafka 토픽에 발행
     */
    public CompletableFuture<SendResult<String, OrderCompletedEventDto>> publishOrderCompletedEvent(OrderCompletedEventDto eventDto) {
        String key = String.valueOf(eventDto.getOrderId());
        
        log.info("주문 완료 이벤트 Kafka 발행 시작 - 주문 ID: {}, 토픽: {}", 
                eventDto.getOrderId(), KafkaTopicConstants.ORDER_COMPLETED);
        
        CompletableFuture<SendResult<String, OrderCompletedEventDto>> future = 
                kafkaTemplate.send(KafkaTopicConstants.ORDER_COMPLETED, key, eventDto);
        
        future.whenComplete((result, throwable) -> {
            if (throwable == null) {
                log.info("주문 완료 이벤트 Kafka 발행 성공 - 주문 ID: {}, 파티션: {}, 오프셋: {}", 
                        eventDto.getOrderId(), 
                        result.getRecordMetadata().partition(), 
                        result.getRecordMetadata().offset());
            } else {
                log.error("주문 완료 이벤트 Kafka 발행 실패 - 주문 ID: {}", 
                        eventDto.getOrderId(), throwable);
                
                sendToDlq(eventDto, throwable.getMessage());
            }
        });
        
        return future;
    }

    private void sendToDlq(OrderCompletedEventDto originalEvent, String failureReason) {
        try {
            OrderCompletedDlqEventDto dlqEvent = OrderCompletedDlqEventDto.createDlqEvent(originalEvent, failureReason);

            String key = String.valueOf(originalEvent.getOrderId());
            
            log.info("DLQ로 실패 메시지 전송 - 주문 ID: {}, 실패 원인: {}", 
                    originalEvent.getOrderId(), failureReason);
            
            dlqKafkaTemplate.send(KafkaTopicConstants.ORDER_COMPLETED_DLQ, key, dlqEvent)
                    .whenComplete((result, throwable) -> {
                        if (throwable == null) {
                            log.info("DLQ 전송 성공 - 주문 ID: {}, 파티션: {}, 오프셋: {}", 
                                    originalEvent.getOrderId(),
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                        } else {
                            log.error("DLQ 전송 실패 - 주문 ID: {}", 
                                    originalEvent.getOrderId(), throwable);
                        }
                    });
        } catch (Exception e) {
            log.error("DLQ 전송 중 예외 발생 - 주문 ID: {}", originalEvent.getOrderId(), e);
        }
    }
}
