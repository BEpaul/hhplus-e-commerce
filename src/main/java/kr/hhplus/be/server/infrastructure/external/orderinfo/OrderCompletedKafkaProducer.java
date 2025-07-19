package kr.hhplus.be.server.infrastructure.external.orderinfo;

import kr.hhplus.be.server.domain.order.event.dto.OrderCompletedEventDto;
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

    public OrderCompletedKafkaProducer(@Qualifier("orderCompletedKafkaTemplate") KafkaTemplate<String, OrderCompletedEventDto> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
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
            }
        });
        
        return future;
    }
}
