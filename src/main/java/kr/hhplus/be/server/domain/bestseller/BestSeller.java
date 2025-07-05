package kr.hhplus.be.server.domain.bestseller;

import jakarta.persistence.*;
import kr.hhplus.be.server.common.config.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "bestseller")
public class BestSeller extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bestseller_id")
    private Long id;
    private String name;
    private Long productId;
    private Long price;
    private Long ranking;
    private LocalDateTime topDate;

    @Builder
    public BestSeller(Long id, String name, Long productId, Long price, Long ranking, LocalDateTime topDate) {
        this.id = id;
        this.name = name;
        this.productId = productId;
        this.price = price;
        this.ranking = ranking;
        this.topDate = topDate;
    }
}
