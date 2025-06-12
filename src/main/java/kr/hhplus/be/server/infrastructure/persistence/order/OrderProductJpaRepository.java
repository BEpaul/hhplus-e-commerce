package kr.hhplus.be.server.infrastructure.persistence.order;

import kr.hhplus.be.server.domain.order.OrderProduct;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderProductJpaRepository extends JpaRepository<OrderProduct, Long> {
}
