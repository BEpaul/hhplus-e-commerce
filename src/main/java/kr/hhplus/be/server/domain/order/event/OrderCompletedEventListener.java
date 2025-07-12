package kr.hhplus.be.server.domain.order.event;

import kr.hhplus.be.server.infrastructure.external.orderinfo.DataPlatform;
import kr.hhplus.be.server.interfaces.web.order.dto.event.OrderCompletedEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCompletedEventListener {

    private final DataPlatform dataPlatform;

    /**
     * 주문 완료 이벤트 처리
     */
    @Async("eventTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderCompletedEvent(OrderCompletedEvent event) {
        try {
            log.info("주문 완료 이벤트 처리 시작 - 주문 ID: {}, 사용자 ID: {}",
                    event.getOrder().getId(), event.getOrder().getUserId());

            OrderCompletedEventDto eventDto = OrderCompletedEventDto.from(event);

            boolean isSuccess = dataPlatform.sendOrderData(eventDto);

            if (!isSuccess) {
                log.error("데이터 플랫폼 전송 실패 - 주문 ID: {}, 사용자 ID: {}",
                        event.getOrder().getId(), event.getOrder().getUserId());
                return;
            }

            log.info("데이터 플랫폼 전송 완료 - 주문 ID: {}, 사용자 ID: {}",
                    event.getOrder().getId(), event.getOrder().getUserId());

        } catch (Exception e) {
            log.error("주문 완료 이벤트 처리 중 오류 발생 - 주문 ID: {}",
                    event.getOrder().getId(), e);
        }
    }

    /**
     * 트랜잭션 롤백 발생 시 주문 완료 이벤트 처리
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    public void handleOrderCompletedEventAfterRollback(OrderCompletedEvent event) {
        log.warn("주문 완료 이벤트가 트랜잭션 롤백로 인해 처리되지 않음 - 주문 ID: {}, 사용자 ID: {}, 롤백 시간: {}",
                event.getOrder().getId(), event.getOrder().getUserId(), LocalDateTime.now());
    }

} 