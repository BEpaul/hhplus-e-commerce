package kr.hhplus.be.server.application.bestseller;

import kr.hhplus.be.server.application.product.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.LocalDate;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BestSellerSchedulerTest {

    @Mock
    private BestSellerRankingService bestSellerRankingService;

    @Mock
    private ProductService productService;

    @InjectMocks
    private BestSellerScheduler bestSellerScheduler;

    private LocalDate today;
    private LocalDate yesterday;

    @BeforeEach
    void setUp() {
        today = LocalDate.now();
        yesterday = today.minusDays(1);
    }

    @Test
    void 새로운_날짜_랭킹_초기화에_성공한다() {
        // given
        when(bestSellerRankingService.getAllProductSales(any())).thenReturn(Set.of());
        
        // when
        bestSellerScheduler.dailyRankingScheduler();
        
        // then
        verify(bestSellerRankingService).initializeDailyRanking(today);
        verify(bestSellerRankingService).getAllProductSales(yesterday);
    }

    @Test
    void 전날_데이터가_RDB에_정상적으로_반영된다() {
        // given
        when(bestSellerRankingService.getAllProductSales(yesterday)).thenReturn(Set.of());
        
        // when
        bestSellerScheduler.dailyRankingScheduler();
        
        // then
        verify(bestSellerRankingService).initializeDailyRanking(today);
        verify(bestSellerRankingService).getAllProductSales(yesterday);
        verify(productService, never()).updateDailySalesCount(any());
    }

    @Test
    void 전날_데이터가_있으면_ProductService를_호출한다() {
        // given
        ZSetOperations.TypedTuple<Object> mockTuple1 = mock(ZSetOperations.TypedTuple.class);
        ZSetOperations.TypedTuple<Object> mockTuple2 = mock(ZSetOperations.TypedTuple.class);
        
        when(mockTuple1.getValue()).thenReturn("product:1");
        when(mockTuple1.getScore()).thenReturn(10.0);
        when(mockTuple2.getValue()).thenReturn("product:2");
        when(mockTuple2.getScore()).thenReturn(5.0);
        
        Set<ZSetOperations.TypedTuple<Object>> mockSalesData = Set.of(mockTuple1, mockTuple2);
        when(bestSellerRankingService.getAllProductSales(yesterday)).thenReturn(mockSalesData);
        
        // when
        bestSellerScheduler.dailyRankingScheduler();
        
        // then
        verify(bestSellerRankingService).initializeDailyRanking(today);
        verify(bestSellerRankingService).getAllProductSales(yesterday);
        verify(productService).updateDailySalesCount(any());
    }

    @Test
    void 전날_데이터가_없으면_업데이트를_스킵한다() {
        // given
        when(bestSellerRankingService.getAllProductSales(yesterday)).thenReturn(Set.of());
        
        // when
        bestSellerScheduler.dailyRankingScheduler();
        
        // then
        verify(bestSellerRankingService).initializeDailyRanking(today);
        verify(bestSellerRankingService).getAllProductSales(yesterday);
        verify(productService, never()).updateDailySalesCount(any());
    }
}