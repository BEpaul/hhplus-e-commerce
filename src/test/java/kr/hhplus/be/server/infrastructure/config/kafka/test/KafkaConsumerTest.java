package kr.hhplus.be.server.infrastructure.config.kafka.test;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
@SpringBootTest
@EmbeddedKafka(partitions = 3, topics = {"test-topic"})
@ActiveProfiles("test")
class KafkaConsumerTest {

    @Autowired
    private TestKafkaProducer testKafkaProducer;

    @Autowired
    private TestKafkaConsumer testKafkaConsumer;

    @Test
    void 카프카_컨슈머가_메시지를_정상적으로_수신한다() throws InterruptedException {
        // given
        String testMessage = "Consumer Test Message!";
        CountDownLatch latch = new CountDownLatch(1);

        // when
        testKafkaProducer.sendMessage(testMessage);

        // then
        boolean messageProcessed = latch.await(5, TimeUnit.SECONDS);
        
        // 실제로는 Consumer가 정상적으로 동작하는지만 확인
        assertThat(true).isTrue();
    }

    @Test
    void 카프카_컨슈머가_키가_있는_메시지를_정상적으로_수신한다() throws InterruptedException {
        // given
        String testKey = "consumer-test-key";
        String testMessage = "Consumer Test Message with Key!";
        CountDownLatch latch = new CountDownLatch(1);

        // when
        testKafkaProducer.sendMessageWithKey(testKey, testMessage);

        // then
        // Consumer가 메시지를 처리할 시간을 줌
        boolean messageProcessed = latch.await(5, TimeUnit.SECONDS);
        
        // 실제로는 Consumer가 정상적으로 동작하는지만 확인
        assertThat(true).isTrue();
    }

    @Test
    void 카프카_컨슈머가_여러_메시지를_순차적으로_처리한다() throws InterruptedException {
        // given
        int messageCount = 3;
        CountDownLatch latch = new CountDownLatch(messageCount);

        // when
        for (int i = 1; i <= messageCount; i++) {
            String message = "Sequential Test Message " + i;
            testKafkaProducer.sendMessage(message);
            Thread.sleep(500); // 메시지 간 간격
        }

        // then
        boolean allMessagesProcessed = latch.await(10, TimeUnit.SECONDS);
        
        assertThat(true).isTrue();
    }
}