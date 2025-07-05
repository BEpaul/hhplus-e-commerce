package kr.hhplus.be.server.domain.product;

import jakarta.persistence.*;
import kr.hhplus.be.server.common.config.BaseTimeEntity;
import kr.hhplus.be.server.common.exception.ApiException;
import lombok.*;

import static kr.hhplus.be.server.common.exception.ErrorCode.OUT_OF_STOCK_PRODUCT;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false)
    private Long price;

    @Column(nullable = false)
    private Long stock;

    @Column(nullable = false)
    private Long salesCount;

    @Column(nullable = false, length = 300)
    private String description;

    @Builder
    public Product(Long id, String name, Long price, Long stock, Long salesCount, String description) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.stock = stock;
        this.salesCount = salesCount;
        this.description = description;
    }

    public void decreaseStock(Long quantity) {
        if (this.stock < quantity) {
            throw new ApiException(OUT_OF_STOCK_PRODUCT);
        }
        this.stock -= quantity;
    }
}
