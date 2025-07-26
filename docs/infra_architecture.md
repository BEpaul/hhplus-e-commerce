# 인프라 구성도

## 시스템 구성 요소

```mermaid
graph TB
    subgraph "Client Layer"
        Web[클라이언트]
    end
    
    subgraph "Application Layer"
        CouponService[쿠폰 서비스<br/>Kafka 기반]
        OrderService[주문 서비스<br/>분산락 기반]
        PaymentService[결제 서비스<br/>분산락 기반]
        ProductService[상품 서비스]
        PointService[포인트 서비스<br/>분산락 기반]
        BestSellerService[베스트셀러 서비스<br/>Redis Sorted Set]
    end
    
    subgraph "Infrastructure Layer"
        Redis[(Redis<br/>캐시/분산락/랭킹)]
        Kafka[(Kafka Cluster<br/>3개 Broker)]
        MySQL[(MySQL 8.0<br/>메인 DB)]
    end
    
    subgraph "Monitoring"
        Grafana[Grafana<br/>대시보드]
        InfluxDB[InfluxDB<br/>메트릭 저장]
        K6[K6<br/>부하 테스트]
    end
    
    subgraph "Container Orchestration"
        Docker[Docker Engine]
        Compose[Docker Compose]
    end
    
    Web --> CouponService
    Web --> OrderService
    Web --> PaymentService
    Web --> ProductService
    Web --> PointService
    Web --> BestSellerService
    
    CouponService --> Kafka
    OrderService --> MySQL
    OrderService --> Redis
    PaymentService --> MySQL
    PaymentService --> Redis
    ProductService --> MySQL
    PointService --> MySQL
    PointService --> Redis
    BestSellerService --> Redis
    BestSellerService --> MySQL
    
    Kafka --> MySQL
    
    K6 --> InfluxDB
    InfluxDB --> Grafana
    
    Docker --> Redis
    Docker --> Kafka
    Docker --> MySQL
    Docker --> Grafana
    Docker --> InfluxDB
    Docker --> K6
    Compose --> Docker
    
    style Redis fill:#ff9999
    style Kafka fill:#99ccff
    style MySQL fill:#99ff99
    style CouponService fill:#ffcc99
    style OrderService fill:#cc99ff
    style BestSellerService fill:#ff99cc
    style Docker fill:#2496ed
    style Compose fill:#2496ed
```

### 설명
- Redis는 상위 상품 캐싱 / 쿠폰 발급 시 선착순 중복 방지 용도로 사용합니다.
- 상위 상품(베스트셀러) 정보 갱신은 매일 자정 00:00에 진행됩니다.
- Kafka는 비동기 이벤트 처리 및 서비스 간 통신에 사용됩니다.
- 분산락은 Redisson을 사용하여 동시성 문제를 해결합니다.
- 모든 컴포넌트들은 Docker 컨테이너로 관리되며, Docker Compose를 통해 로컬 개발 환경을 구성합니다.