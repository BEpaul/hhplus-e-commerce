package kr.hhplus.be.server.infrastructure.config.kafka.test;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class TestKafkaProducer {

    private final KafkaTemplate<String, String> stringKafkaTemplate;
    
    private static final String TEST_TOPIC = "test-topic";

    public void sendMessage(String message) {
        log.info("Sending message to topic {}: {}", TEST_TOPIC, message);
        
        CompletableFuture<SendResult<String, String>> future = stringKafkaTemplate.send(TEST_TOPIC, message);
        
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Message sent successfully to topic {} partition {} offset {}", 
                    result.getRecordMetadata().topic(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());
            } else {
                log.error("Failed to send message to topic {}: {}", TEST_TOPIC, ex.getMessage());
            }
        });
    }

    public void sendMessageWithKey(String key, String message) {
        log.info("Sending message with key {} to topic {}: {}", key, TEST_TOPIC, message);
        
        CompletableFuture<SendResult<String, String>> future = stringKafkaTemplate.send(TEST_TOPIC, key, message);
        
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Message with key {} sent successfully to topic {} partition {} offset {}", 
                    key,
                    result.getRecordMetadata().topic(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());
            } else {
                log.error("Failed to send message with key {} to topic {}: {}", key, TEST_TOPIC, ex.getMessage());
            }
        });
    }
}
