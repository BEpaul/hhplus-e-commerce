package kr.hhplus.be.server.infrastructure.persistence.product;

import kr.hhplus.be.server.domain.product.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    /**
     * 비관적 락을 사용한 상품 조회
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :productId")
    Optional<Product> findByIdWithPessimisticLock(@Param("productId") Long productId);
    
    /**
     * 상품 판매량 업데이트
     */
    @Query("UPDATE Product p SET p.salesCount = p.salesCount + :additionalSales WHERE p.id = :productId")
    @Modifying
    @Transactional
    void updateSalesCount(@Param("productId") Long productId, @Param("additionalSales") Long additionalSales);
}
