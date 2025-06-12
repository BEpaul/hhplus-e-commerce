package kr.hhplus.be.server.infrastructure.external;

import kr.hhplus.be.server.domain.payment.Payment;
import org.springframework.stereotype.Component;

@Component
public class DataPlatform {

    public boolean sendData(Payment payment) {
        return true;
    }
}
