package kr.hhplus.be.server.infrastructure.persistence.order;

import kr.hhplus.be.server.domain.order.OrderProduct;
import kr.hhplus.be.server.domain.order.OrderProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class OrderProductRepositoryImpl implements OrderProductRepository {

    private final OrderProductJpaRepository orderProductJpaRepository;

    @Override
    public void save(OrderProduct orderProduct) {
        orderProductJpaRepository.save(orderProduct);
    }
}
