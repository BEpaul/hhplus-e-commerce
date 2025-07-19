package kr.hhplus.be.server.infrastructure.config.kafka.test;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TestKafkaConsumer {

    private static final String TEST_TOPIC = "test-topic";

    @KafkaListener(
        topics = TEST_TOPIC,
        groupId = "test-group",
        containerFactory = "stringKafkaListenerContainerFactory"
    )
    public void consumeMessage(
            @Payload String message,
            Acknowledgment acknowledgment) {
        
        log.info("Received message: {}", message);
        
        processMessage(message, null);
        
        // 수동 커밋
        acknowledgment.acknowledge();
    }

    private void processMessage(String message, String key) {
        log.info("Processing message: {} with key: {}", message, key);

        try {
            // ** 비즈니스 로직 작성 가능 ! **
            Thread.sleep(100);
            log.info("Message processed successfully: {}", message);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Message processing interrupted: {}", e.getMessage());
        }
    }

    // 상세 정보가 필요한 경우 사용할 수 있는 메서드
    @KafkaListener(
        topics = TEST_TOPIC,
        groupId = "test-group-detailed",
        containerFactory = "stringKafkaListenerContainerFactory"
    )
    public void consumeMessageDetailed(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        try {
            log.info("Received message from topic {} partition {} offset {} key {}: {}", 
                record.topic(), record.partition(), record.offset(), record.key(), record.value());
            
            processMessage(record.value(), record.key());
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error processing message: {}", e.getMessage(), e);
            // 에러 처리 로직 (retry, DLQ 등)
        }
    }
}
