package kr.hhplus.be.server.infrastructure.config.kafka.test;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/kafka/test")
@RequiredArgsConstructor
public class TestKafkaController {

    private final TestKafkaProducer testKafkaProducer;

    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> sendMessage(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        String key = request.get("key");
        
        if (message == null || message.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Message cannot be empty"));
        }

        try {
            if (key != null && !key.trim().isEmpty()) {
                testKafkaProducer.sendMessageWithKey(key, message);
            } else {
                testKafkaProducer.sendMessage(message);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Message sent to Kafka");
            response.put("timestamp", LocalDateTime.now());
            response.put("sentMessage", message);
            if (key != null) {
                response.put("key", key);
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to send message to Kafka", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to send message: " + e.getMessage()));
        }
    }

    @PostMapping("/send/simple")
    public ResponseEntity<Map<String, Object>> sendSimpleMessage(@RequestParam String message) {
        try {
            testKafkaProducer.sendMessage(message);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Simple message sent to Kafka");
            response.put("timestamp", LocalDateTime.now());
            response.put("sentMessage", message);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to send simple message to Kafka", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to send message: " + e.getMessage()));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "healthy");
        response.put("timestamp", LocalDateTime.now());
        response.put("kafka", "configured");
        
        return ResponseEntity.ok(response);
    }
} 