package kr.hhplus.be.server.application.bestseller;

import kr.hhplus.be.server.application.product.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class BestSellerScheduler {
    
    private final BestSellerRankingService bestSellerRankingService;
    private final ProductService productService;
    
    /**
     * 매일 자정 00:00:00에 실행 (KST 기준)
     * 1. 새로운 날짜 랭킹 초기화
     * 2. 전날 Redis 데이터를 RDB에 반영
     */
    @Scheduled(cron = "0 0 0 * * ?", zone = "Asia/Seoul")
    @Transactional
    public void dailyRankingScheduler() {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        
        log.info("=== 베스트셀러 일일 스케줄러 시작 ===");
        log.info("오늘 날짜: {}, 어제 날짜: {}", today, yesterday);
        
        try {
            // 1. 새로운 날짜 랭킹 초기화
            log.info("1단계: 새로운 날짜 랭킹 초기화 시작");
            bestSellerRankingService.initializeDailyRanking(today);
            log.info("1단계: 새로운 날짜 랭킹 초기화 완료");
            
            // 2. 전날 Redis 데이터를 RDB에 반영
            log.info("2단계: 전날 데이터 RDB 반영 시작");
            reflectYesterdayDataToRDB(yesterday);
            log.info("2단계: 전날 데이터 RDB 반영 완료");
            
            log.info("=== 베스트셀러 일일 스케줄러 완료 ===");
            
        } catch (Exception e) {
            log.error("베스트셀러 일일 스케줄러 실행 중 오류 발생", e);
            throw e;
        }
    }
    
    /**
     * 전날 Redis 데이터를 RDB 상품(Product) 테이블의 판매량(salesCount)에 반영
     */
    private void reflectYesterdayDataToRDB(LocalDate yesterday) {
        Set<ZSetOperations.TypedTuple<Object>> yesterdaySales = 
                bestSellerRankingService.getAllProductSales(yesterday);
        
        if (yesterdaySales.isEmpty()) {
            log.warn("어제 판매 데이터가 없습니다. 날짜: {}", yesterday);
            return;
        }
        
        log.info("어제 판매 데이터 {}건을 RDB에 반영합니다.", yesterdaySales.size());
        
        Map<Long, Long> productSalesMap = new HashMap<>();
        
        for (ZSetOperations.TypedTuple<Object> tuple : yesterdaySales) {
            String member = (String) tuple.getValue();
            Double salesCount = tuple.getScore();
            
            if (member == null || salesCount == null || salesCount == 0) {
                continue;
            }
            
            // "product:123" 형태에서 상품 ID 추출
            String productIdStr = member.replace("product:", "");
            try {
                Long productId = Long.parseLong(productIdStr);
                productSalesMap.put(productId, salesCount.longValue());
                
            } catch (NumberFormatException e) {
                log.warn("잘못된 상품 ID 형식: {}", member);
            }
        }
        
        // ProductService를 통해 일괄 업데이트
        productService.updateDailySalesCount(productSalesMap);
    }
} 