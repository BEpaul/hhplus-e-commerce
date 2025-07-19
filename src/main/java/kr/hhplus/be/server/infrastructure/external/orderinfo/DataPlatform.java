package kr.hhplus.be.server.infrastructure.external.orderinfo;

import kr.hhplus.be.server.domain.order.event.dto.OrderCompletedEventDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DataPlatform {

    /**
     * 주문 완료 데이터를 데이터 플랫폼에 전송
     */
    public boolean sendOrderData(OrderCompletedEventDto orderEvent) {
        // 실제 외부 API 호출 (Mock)
        try {
            // externalApiClient.sendOrderData(orderEvent);
            // 실제 구현에서는 HTTP 클라이언트를 사용하여 외부 API 호출
            log.info("데이터 플랫폼으로 주문 정보 전송: {}", orderEvent.getOrderId());
            return true;
        } catch (Exception e) {
            log.warn("데이터 플랫폼 전송 실패: {}", e.getMessage());
            return false;
        }
    }
}
