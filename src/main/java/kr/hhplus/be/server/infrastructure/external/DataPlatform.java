package kr.hhplus.be.server.infrastructure.external;

import kr.hhplus.be.server.domain.payment.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
//@RequiredArgsConstructor
public class DataPlatform {

//    private final OutBoxEventRepository outBoxEventRepository;

    public boolean sendData(Payment payment) {
        // 실제 외부 API 호출
        try {
            // externalApiClient.send(payment);
            return true;
        } catch (Exception e) {
            // 실패시 outbox에 저장
//            outboxRepository.save(OutboxEvent.create(payment));
            // 또는 재시도 로직
            return false;
        }
    }
}
