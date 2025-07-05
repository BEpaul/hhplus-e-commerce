package kr.hhplus.be.server.application.bestseller;

import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.infrastructure.persistence.product.ProductRepository;
import kr.hhplus.be.server.interfaces.web.product.dto.response.ProductRankingInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@SpringBootTest
@ActiveProfiles("test")
class BestSellerRankingServiceTest {

    @Autowired
    private BestSellerRankingService bestSellerRankingService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @MockitoBean
    private ProductRepository productRepository;

    private ZSetOperations<String, Object> zSetOperations;

    @BeforeEach
    void setUp() {
        zSetOperations = redisTemplate.opsForZSet();
        // Redis 데이터 초기화
        redisTemplate.getConnectionFactory().getConnection().flushAll();
    }

    @Test
    void 새로운_날짜_랭킹_초기화에_성공한다() {
        // given
        LocalDate testDate = LocalDate.of(2024, 1, 15);
        List<Product> mockProducts = List.of(
                Product.builder().id(1L).name("상품1").price(1000L).stock(100L).build(),
                Product.builder().id(2L).name("상품2").price(2000L).stock(50L).build(),
                Product.builder().id(3L).name("상품3").price(1500L).stock(80L).build()
        );
        given(productRepository.findAll()).willReturn(mockProducts);

        // when
        bestSellerRankingService.initializeDailyRanking(testDate);

        // then
        String rankingKey = "ranking:daily:2024-01-15";
        assertThat(redisTemplate.hasKey(rankingKey)).isTrue();
        assertThat(zSetOperations.size(rankingKey)).isEqualTo(3);
        assertThat(zSetOperations.score(rankingKey, "product:1")).isEqualTo(0.0);
        assertThat(zSetOperations.score(rankingKey, "product:2")).isEqualTo(0.0);
        assertThat(zSetOperations.score(rankingKey, "product:3")).isEqualTo(0.0);
        
        // TTL 확인 (31일)
        Long ttl = redisTemplate.getExpire(rankingKey);
        assertThat(ttl).isGreaterThan(30L * 24 * 60 * 60); // 30일 이상
    }

    @Test
    void 상품_판매량_증가에_성공한다() {
        // given
        LocalDate testDate = LocalDate.of(2024, 1, 15);
        String rankingKey = "ranking:daily:2024-01-15";
        String member = "product:1";
        
        // 초기 데이터 설정
        zSetOperations.add(rankingKey, member, 0);

        // when
        bestSellerRankingService.incrementSalesCount(testDate, 1L, 5L);

        // then
        assertThat(zSetOperations.score(rankingKey, member)).isEqualTo(5.0);
    }

    @Test
    void 오늘_판매량_증가에_성공한다() {
        // given
        LocalDate today = LocalDate.now();
        String rankingKey = "ranking:daily:" + today.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String member = "product:1";
        
        // 초기 데이터 설정
        zSetOperations.add(rankingKey, member, 0);

        // when
        bestSellerRankingService.incrementTodaySales(1L, 3L);

        // then
        assertThat(zSetOperations.score(rankingKey, member)).isEqualTo(3.0);
    }

    @Test
    void 베스트셀러_TOP5_조회에_성공한다() {
        // given

        String rankingKey = "ranking:daily:" + LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        
        // 테스트 데이터 설정 (판매량 순서: 상품3 > 상품1 > 상품2)
        zSetOperations.add(rankingKey, "product:1", 100);
        zSetOperations.add(rankingKey, "product:2", 50);
        zSetOperations.add(rankingKey, "product:3", 200);
        zSetOperations.add(rankingKey, "product:4", 30);
        zSetOperations.add(rankingKey, "product:5", 10);

        // when
        List<ProductRankingInfo> result = bestSellerRankingService.getTopProductsWithRanking();

        // then
        assertThat(result).hasSize(5);
        
        // 판매량 순서대로 정렬되어야 함 (내림차순)
        assertThat(result.get(0).getProductId()).isEqualTo(3L);
        assertThat(result.get(0).getSalesCount()).isEqualTo(200L);
        assertThat(result.get(0).getRanking()).isEqualTo(1L);
        
        assertThat(result.get(1).getProductId()).isEqualTo(1L);
        assertThat(result.get(1).getSalesCount()).isEqualTo(100L);
        assertThat(result.get(1).getRanking()).isEqualTo(2L);
        
        assertThat(result.get(2).getProductId()).isEqualTo(2L);
        assertThat(result.get(2).getSalesCount()).isEqualTo(50L);
        assertThat(result.get(2).getRanking()).isEqualTo(3L);
        
        assertThat(result.get(3).getProductId()).isEqualTo(4L);
        assertThat(result.get(3).getSalesCount()).isEqualTo(30L);
        assertThat(result.get(3).getRanking()).isEqualTo(4L);
        
        assertThat(result.get(4).getProductId()).isEqualTo(5L);
        assertThat(result.get(4).getSalesCount()).isEqualTo(10L);
        assertThat(result.get(4).getRanking()).isEqualTo(5L);
    }

    @Test
    void 베스트셀러_데이터가_없을_때_빈_리스트를_반환한다() {
        // given
        LocalDate testDate = LocalDate.of(2024, 1, 15);
        String rankingKey = "ranking:daily:2024-01-15";

        // when
        List<ProductRankingInfo> result = bestSellerRankingService.getTopProductsWithRanking();

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void 전체_상품_판매량_조회에_성공한다() {
        // given
        LocalDate testDate = LocalDate.of(2024, 1, 15);
        String rankingKey = "ranking:daily:2024-01-15";
        
        // 테스트 데이터 설정
        zSetOperations.add(rankingKey, "product:1", 100);
        zSetOperations.add(rankingKey, "product:2", 50);
        zSetOperations.add(rankingKey, "product:3", 200);

        // when
        Set<ZSetOperations.TypedTuple<Object>> result = bestSellerRankingService.getAllProductSales(testDate);

        // then
        assertThat(result).hasSize(3);
        
        boolean hasProduct1 = result.stream().anyMatch(tuple ->
                tuple.getValue().toString().equals("product:1") && tuple.getScore().equals(100.0));
        boolean hasProduct2 = result.stream().anyMatch(tuple -> 
                tuple.getValue().toString().equals("product:2") && tuple.getScore().equals(50.0));
        boolean hasProduct3 = result.stream().anyMatch(tuple -> 
                tuple.getValue().toString().equals("product:3") && tuple.getScore().equals(200.0));
        
        assertThat(hasProduct1).isTrue();
        assertThat(hasProduct2).isTrue();
        assertThat(hasProduct3).isTrue();
    }

    @Test
    void 상품이_없을_때_초기화를_건너뛴다() {
        // given
        LocalDate testDate = LocalDate.of(2024, 1, 15);
        given(productRepository.findAll()).willReturn(List.of());

        // when
        bestSellerRankingService.initializeDailyRanking(testDate);

        // then
        String rankingKey = "ranking:daily:2024-01-15";
        assertThat(redisTemplate.hasKey(rankingKey)).isFalse();
    }

    @Test
    void 동일_상품_여러번_판매량_증가에_성공한다() {
        // given
        LocalDate testDate = LocalDate.of(2024, 1, 15);
        String rankingKey = "ranking:daily:2024-01-15";
        String member = "product:1";
        
        // 초기 데이터 설정
        zSetOperations.add(rankingKey, member, 0);

        // when
        bestSellerRankingService.incrementSalesCount(testDate, 1L, 5L);
        bestSellerRankingService.incrementSalesCount(testDate, 1L, 3L);
        bestSellerRankingService.incrementSalesCount(testDate, 1L, 2L);

        // then
        assertThat(zSetOperations.score(rankingKey, member)).isEqualTo(10.0);
    }
}
