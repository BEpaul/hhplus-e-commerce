package kr.hhplus.be.server.domain.product;

import jakarta.persistence.*;
import kr.hhplus.be.server.common.BaseTimeEntity;
import kr.hhplus.be.server.common.exception.OutOfStockException;
import lombok.*;

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

    @Column(nullable = false, length = 300)
    private String description;

    @Builder
    public Product(Long id, String name, Long price, Long stock, String description) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.stock = stock;
        this.description = description;
    }

    public void decreaseStock(Long quantity) {
        if (this.stock < quantity) {
            throw new OutOfStockException("상품의 재고가 부족합니다.");
        }
        this.stock -= quantity;
    }
}
