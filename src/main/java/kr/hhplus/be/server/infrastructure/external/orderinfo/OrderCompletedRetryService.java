package kr.hhplus.be.server.infrastructure.external.orderinfo;

import kr.hhplus.be.server.domain.order.event.dto.OrderCompletedDlqEventDto;
import kr.hhplus.be.server.domain.order.event.dto.OrderCompletedEventDto;
import kr.hhplus.be.server.infrastructure.config.kafka.KafkaTopicConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class OrderCompletedRetryService {

    private final KafkaTemplate<String, OrderCompletedEventDto> kafkaTemplate;
    private final KafkaTemplate<String, OrderCompletedDlqEventDto> dlqKafkaTemplate;

    public OrderCompletedRetryService(
            @org.springframework.beans.factory.annotation.Qualifier("orderCompletedKafkaTemplate") KafkaTemplate<String, OrderCompletedEventDto> kafkaTemplate,
            @org.springframework.beans.factory.annotation.Qualifier("orderCompletedDlqKafkaTemplate") KafkaTemplate<String, OrderCompletedDlqEventDto> dlqKafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        this.dlqKafkaTemplate = dlqKafkaTemplate;
    }

    @Async
    public CompletableFuture<SendResult<String, OrderCompletedEventDto>> retryFromDlq(OrderCompletedDlqEventDto dlqEvent) {
        OrderCompletedEventDto originalEvent = dlqEvent.getOriginalEvent();
        String key = String.valueOf(originalEvent.getOrderId());
        
        log.info("DLQ에서 재처리 시작 - 주문 ID: {}, 재시도 횟수: {}", 
                originalEvent.getOrderId(), dlqEvent.getRetryCount());
        
        CompletableFuture<SendResult<String, OrderCompletedEventDto>> future = 
                kafkaTemplate.send(KafkaTopicConstants.ORDER_COMPLETED, key, originalEvent);
        
        future.whenComplete((result, throwable) -> {
            if (throwable == null) {
                log.info("DLQ 재처리 성공 - 주문 ID: {}, 파티션: {}, 오프셋: {}", 
                        originalEvent.getOrderId(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("DLQ 재처리 실패 - 주문 ID: {}, 재시도 횟수: {}", 
                        originalEvent.getOrderId(), dlqEvent.getRetryCount(), throwable);
                
                handleRetryFailure(dlqEvent, throwable.getMessage());
            }
        });
        
        return future;
    }

    private void handleRetryFailure(OrderCompletedDlqEventDto dlqEvent, String failureReason) {
        dlqEvent.incrementRetryCount();
        
        if (dlqEvent.canRetry()) {
            log.info("재시도 가능 - 주문 ID: {}, 재시도 횟수: {}", 
                    dlqEvent.getOriginalEvent().getOrderId(), dlqEvent.getRetryCount());
            
            scheduleRetry(dlqEvent);
        } else {
            log.error("최대 재시도 횟수 초과 - 주문 ID: {}, 최종 실패 원인: {}", 
                    dlqEvent.getOriginalEvent().getOrderId(), failureReason);
            
            handleFinalFailure(dlqEvent, failureReason);
        }
    }

    @Async
    public void scheduleRetry(OrderCompletedDlqEventDto dlqEvent) {
        try {
            // 지수 백오프: 1초, 2초, 4초 지연
            long delayMs = 1000L * (long) Math.pow(2, dlqEvent.getRetryCount());
            log.info("재시도 지연 시작 - 주문 ID: {}, 지연 시간: {}ms", 
                    dlqEvent.getOriginalEvent().getOrderId(), delayMs);
            
            Thread.sleep(delayMs);
            
            log.info("재시도 지연 완료 - 주문 ID: {}", 
                    dlqEvent.getOriginalEvent().getOrderId());
            
            retryFromDlq(dlqEvent);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("재시도 지연 중 인터럽트 발생 - 주문 ID: {}", 
                    dlqEvent.getOriginalEvent().getOrderId(), e);
        }
    }

    private void handleFinalFailure(OrderCompletedDlqEventDto dlqEvent, String failureReason) {
        log.error("주문 완료 이벤트 최종 실패 - 주문 ID: {}, 실패 원인: {}, 실패 시간: {}", 
                dlqEvent.getOriginalEvent().getOrderId(),
                failureReason,
                dlqEvent.getFailedAt());
        
        // TODO: 관리자 알림 로직 추가
        // TODO: 실패 통계 수집 로직 추가
        // TODO: 데이터베이스에 실패 기록 저장 로직 추가
        
        // 최종 실패 시 별도 토픽으로 전송하여 모니터링 시스템에서 처리할 수 있도록 함
        sendToFinalFailureTopic(dlqEvent, failureReason);
    }

    private void sendToFinalFailureTopic(OrderCompletedDlqEventDto dlqEvent, String failureReason) {
        try {
            String key = String.valueOf(dlqEvent.getOriginalEvent().getOrderId());
            
            log.info("최종 실패 토픽으로 전송 - 주문 ID: {}", 
                    dlqEvent.getOriginalEvent().getOrderId());
            
            dlqKafkaTemplate.send(KafkaTopicConstants.ORDER_COMPLETED_DLQ, key, dlqEvent)
                    .whenComplete((result, throwable) -> {
                        if (throwable == null) {
                            log.info("최종 실패 토픽 전송 성공 - 주문 ID: {}", 
                                    dlqEvent.getOriginalEvent().getOrderId());
                        } else {
                            log.error("최종 실패 토픽 전송 실패 - 주문 ID: {}", 
                                    dlqEvent.getOriginalEvent().getOrderId(), throwable);
                        }
                    });
        } catch (Exception e) {
            log.error("최종 실패 토픽 전송 중 예외 발생 - 주문 ID: {}", 
                    dlqEvent.getOriginalEvent().getOrderId(), e);
        }
    }
} 