package kr.hhplus.be.server.application.product;

import kr.hhplus.be.server.common.exception.ApiException;
import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.infrastructure.persistence.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static kr.hhplus.be.server.common.exception.ErrorCode.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;

    /**
     * 상품 단건 조회
     */
    public Product getProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ApiException(PRODUCT_NOT_FOUND));
    }

    /**
     * 상품 목록 조회
     */
    public List<Product> getProducts() {
        return productRepository.findAll();
    }

    /**
     * 비관적 락을 사용한 상품 조회 (재고 차감용)
     */
    @Transactional
    public Product getProductWithPessimisticLock(Long productId) {
        return productRepository.findByIdWithPessimisticLock(productId)
                .orElseThrow(() -> new ApiException(PRODUCT_NOT_FOUND));
    }

    /**
     * 상품 ID 목록으로 일괄 조회
     */
    public List<Product> getProductsByIds(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return List.of();
        }
        return productRepository.findByIds(productIds);
    }

    /**
     * 상품 ID로 상품 맵 생성
     */
    public Map<Long, Product> getProductMapByIds(List<Long> productIds) {
        List<Product> products = getProductsByIds(productIds);
        return products.stream()
                .collect(java.util.stream.Collectors.toMap(
                        Product::getId,
                        product -> product
                ));
    }

    /**
     * 상품들의 일일 판매량을 누적 업데이트
     */
    @Transactional
    public void updateDailySalesCount(Map<Long, Long> productSalesMap) {
        if (productSalesMap.isEmpty()) {
            log.warn("업데이트할 판매 데이터가 없습니다.");
            return;
        }
        
        log.info("{}개 상품의 판매량을 업데이트합니다.", productSalesMap.size());
        
        for (Map.Entry<Long, Long> entry : productSalesMap.entrySet()) {
            Long productId = entry.getKey();
            Long additionalSales = entry.getValue();
            
            if (additionalSales > 0) {
                productRepository.updateSalesCount(productId, additionalSales);
                log.debug("상품 {} 판매량 {}건 추가 완료", productId, additionalSales);
            }
        }
    }
    
    /**
     * 특정 상품의 판매량 업데이트
     */
    @Transactional
    public void updateProductSalesCount(Long productId, Long additionalSales) {
        if (additionalSales > 0) {
            productRepository.updateSalesCount(productId, additionalSales);
            log.debug("상품 {} 판매량 {}건 추가 완료", productId, additionalSales);
        }
    }
}
