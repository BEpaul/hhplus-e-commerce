package kr.hhplus.be.server.infrastructure.external.coupon;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CouponOutBoxEventRepository extends JpaRepository<CouponOutBoxEvent, Long> {

    @Query("SELECT e FROM CouponOutBoxEvent e WHERE e.eventType = :eventType AND e.status = 'PENDING' ORDER BY e.createdAt")
    List<CouponOutBoxEvent> findPendingEventsByType(@Param("eventType") String eventType);

    @Query("SELECT e FROM CouponOutBoxEvent e WHERE e.status = 'FAILED' AND e.retryCount < 3 ORDER BY e.createdAt")
    List<CouponOutBoxEvent> findFailedEventsForRetry();
}
