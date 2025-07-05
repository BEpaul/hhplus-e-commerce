package kr.hhplus.be.server.infrastructure.external.payment;

import kr.hhplus.be.server.domain.payment.Payment;
import org.springframework.stereotype.Component;

@Component
//@RequiredArgsConstructor
public class DataPlatform {

//    private final PaymentOutBoxEventRepository paymentOutBoxEventRepository;

    public boolean sendData(Payment payment) {
        // 실제 외부 API 호출
        try {
            // externalApiClient.send(payment);
            return true;
        } catch (Exception e) {
            // 실패시 outbox에 저장
//            paymentOutBoxEventRepository.save(PaymentOutBoxEvent.create(payment));
            // 또는 재시도 로직
            return false;
        }
    }
}
