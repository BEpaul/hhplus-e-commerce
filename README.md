# 🛒 E-commerce Service

> 상품 주문 및 결제 기능을 메인으로 하는 이커머스 서비스 프로젝트

## 📋 Table of Contents

- [Introduction](#-introduction)
- [Tech Stack](#-tech-stack)
- [Getting Started](#-getting-started)
- [Documentation](#-documentation)
- [Project Structure](#-project-structure)

## 🎯 Introduction

이 프로젝트는 상품 주문 및 결제 기능을 메인으로 하는 이커머스 서비스입니다. 
고성능과 확장성을 고려한 아키텍처로 설계되었으며, 동시성 제어, 캐싱, 메시징 등 
다양한 기술을 활용하여 안정적인 서비스를 제공합니다.

## 🛠 Tech Stack

| 분류 | 기술 |
|------|------|
| **언어** | Java 17 |
| **프레임워크** | Spring Boot 3.4.1 |
| **데이터베이스** | MySQL 8.0 |
| **캐시/메시징** | Redis, Apache Kafka |
| **분산락** | Redisson |
| **모니터링** | Grafana, InfluxDB, K6 |
| **컨테이너** | Docker & Docker Compose |

## 🚀 Getting Started

### Prerequisites

- Java 17 이상
- Docker & Docker Compose
- Git

### Local Environment Setup

1. **Clone Repository**
   ```bash
   git clone <repository-url>
   cd server-java
   ```

2. **Run Infrastructure Containers**
   
   `local` 프로파일로 실행하기 위해 필요한 인프라 컨테이너를 실행합니다.
   
   ```bash
   docker-compose up -d && docker-compose -f docker-compose.yml up -d
   ```

3. **Run Application**
   ```bash
   ./gradlew bootRun
   ```

## 📚 Documentation

### 📖 Core Documents
- [요구사항 분석 및 정리](./docs/requirements.md)
- [API 명세서](./docs/api_specification.md)
- [ERD](./docs/erd.md)
- [시퀀스 다이어그램](./docs/sequence_diagram.md)
- [인프라 구성도](./docs/infra_architecture.md)

### 🔧 Technical Documents
- [분산락 설계 및 운영](./docs/lock/distributed_lock.md)
- [Redis 캐시 설계 및 운영](./docs/redis)
- [Kafka 설계 및 운영](./docs/kafka)
- [MSA 설계](./docs/msa_design.md)

### 📊 Testing & Performance
- [동시성 보고서](./docs/concurrency_report)
- [부하 테스트 보고서](./docs/load_test_report/coupon_issuance.md)
- [장애 대응 보고서](./docs/incidence_response/manual.md)

## 📁 Project Structure

```
src/main/java/kr/hhplus/be/server/
├── application/          # 애플리케이션 서비스
│   ├── bestseller/      # 베스트셀러 서비스
│   ├── coupon/          # 쿠폰 서비스
│   ├── order/           # 주문 서비스
│   ├── payment/         # 결제 서비스
│   ├── point/           # 포인트 서비스
│   └── product/         # 상품 서비스
├── domain/              # 도메인 모델
├── infrastructure/      # 인프라스트럭처
│   ├── config/          # 설정
│   ├── external/        # 외부 연동
│   ├── kafka/           # Kafka 관련
│   └── persistence/     # 데이터 접근
└── interfaces/          # 인터페이스
    └── web/             # 웹 컨트롤러
```
