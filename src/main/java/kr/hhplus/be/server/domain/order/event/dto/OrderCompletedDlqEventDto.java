package kr.hhplus.be.server.domain.order.event.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCompletedDlqEventDto {
    
    private OrderCompletedEventDto originalEvent;
    private String failureReason;
    private LocalDateTime failedAt;
    private int retryCount;

    private static final int MAX_RETRY_COUNT = 3;

    public static OrderCompletedDlqEventDto createDlqEvent(
            OrderCompletedEventDto originalEvent,
            String failureReason
    ) {
        return OrderCompletedDlqEventDto.builder()
                .originalEvent(originalEvent)
                .failureReason(failureReason)
                .failedAt(LocalDateTime.now())
                .retryCount(0)
                .build();
    }

    public boolean canRetry() {
        return retryCount < MAX_RETRY_COUNT;
    }
    
    public void incrementRetryCount() {
        this.retryCount++;
    }
} 