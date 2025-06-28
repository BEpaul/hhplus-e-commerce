# API 명세서

## 공통 응답 형식

### 성공 응답
모든 API는 성공 시 다음과 같은 형식으로 응답합니다:
```json
{
  "code": 200,
  "message": "성공 메시지",
  "data": { ... }
}
```

### 에러 응답
에러 발생 시 다음과 같은 형식으로 응답합니다:
```json
{
  "code": "ERROR_CODE",
  "message": "에러 메시지"
}
```

### 주요 에러 코드
| HTTP Status | Error Code | 설명 |
|-------------|------------|------|
| 400 | INVALID_INPUT_VALUE | 잘못된 입력값 |
| 400 | NEGATIVE_CHARGE_POINT | 충전 금액은 0보다 커야 함 |
| 400 | NEGATIVE_USE_POINT | 사용 금액은 0보다 커야 함 |
| 400 | ORDER_PRODUCT_EMPTY | 주문 상품이 비어있음 |
| 400 | NOT_SUPPORTED_DISCOUNT_TYPE | 지원하지 않는 할인 타입 |
| 404 | USER_NOT_FOUND | 사용자를 찾을 수 없음 |
| 404 | PRODUCT_NOT_FOUND | 상품이 존재하지 않음 |
| 404 | COUPON_NOT_FOUND | 쿠폰을 찾을 수 없음 |
| 404 | BESTSELLER_NOT_FOUND | 베스트셀러를 찾을 수 없음 |
| 404 | PAYMENT_INFO_NOT_EXIST | 결제 정보가 없음 |
| 409 | DUPLICATE_PAYMENT | 이미 처리된 결제 요청 |
| 409 | ALREADY_USED_COUPON | 이미 사용된 쿠폰 |
| 409 | ALREADY_APPLIED_COUPON | 이미 적용된 쿠폰 |
| 409 | NOT_OWNED_USER_COUPON | 소유하지 않은 쿠폰 |
| 422 | EXCEEDS_MAXIMUM_POINT | 충전 후 포인트가 300만을 초과 |
| 422 | NOT_ENOUGH_POINT | 포인트가 부족 |
| 422 | OUT_OF_STOCK_PRODUCT | 상품 재고가 부족 |
| 422 | OUT_OF_STOCK_COUPON | 쿠폰 재고가 부족 |
| 422 | EXPIRED_COUPON | 쿠폰이 만료됨 |
| 500 | PAYMENT_PROCESS_ERROR | 결제 처리 중 오류 |
| 500 | PAYMENT_FAILED | 결제 실패 |
| 500 | INTERNAL_SERVER_ERROR | 서버 내부 오류 |

## 1️⃣ 포인트 (Point)
### ✅ 포인트 조회
사용자의 잔여 포인트를 조회한다.
- Method: `GET`
- End point: `/api/v1/users/{userId}/points`

<details markdown="1">
<summary>상세 보기</summary>

### **Request**

**Query parameters**

| Filed  | Type   | Description | Constraints |
|--------|--------|-------------|---------|
| userId | Number | 사용자 ID      | 양의 정수   |


### **Response**

| Field     | Type   | Description       |
|-----------|--------|-------------------|
| code     | Number | 응답 코드  |
| message  | String | 응답 메시지 |
| data.point | Number | 사용자의 현재 포인트 잔액 |

**성공 응답 예시:**
```json
{
  "code": 200,
  "message": "포인트 조회 성공",
  "data": {
    "point": 10000
  }
}
```

**에러 응답 예시:**
```json
{
  "code": "USER_NOT_FOUND",
  "message": "사용자를 찾을 수 없습니다."
}
```
</details>

### ✅ 포인트 충전

사용자의 포인트를 충전한다.

- Method: `POST`
- Endpoint: `/api/v1/points/charge`

<details markdown="1">
<summary>상세 보기</summary>

### **Request**

**Request Body**

| Body | Type | Description | Constraints |
|------|------| ------------|-------------|
| userId | Number | 사용자 ID | 양의 정수 |
| amount | Number | 충전 금액 | 양의 정수, 최대 300만원 |

### **Response**

| Field    | Type   | Description       |
|-----------|--------|-------------------|
| code     | Number | 응답 코드  |
| message  | String | 응답 메시지 |
| data.point | Number | 충전 후 사용자의 현재 포인트 잔액 |

**성공 응답 예시:**
```json
{
  "code": 200,
  "message": "포인트 충전 성공",
  "data": {
    "point": 1300000
  }
}
```

**에러 응답 예시:**
```json
{
  "code": "NEGATIVE_CHARGE_POINT",
  "message": "충전 금액은 0보다 커야 합니다."
}
```

```json
{
  "code": "EXCEEDS_MAXIMUM_POINT",
  "message": "충전 후 포인트가 300만을 초과할 수 없습니다."
}
```

```json
{
  "code": "USER_NOT_FOUND",
  "message": "사용자를 찾을 수 없습니다."
}
```
</details>

## 2️⃣ 상품 (Product)
### ✅ 상품 목록 조회
사용 가능한 상품 목록을 조회한다.

- Method: `GET`
- Endpoint: `/api/v1/products`

<details markdown="1">
<summary>상세 보기</summary>

### **Response**
| Field     | Type   | Description       |
|-----------|--------|-------------------|
| code     | Number | 응답 코드  |
| message  | String | 응답 메시지 |
| data.products | Array  | 상품 목록 |
| data.products[].id | Number | 상품 ID |
| data.products[].name | String | 상품 이름 |
| data.products[].price | Number | 상품 가격 |
| data.products[].stock | Number | 상품 잔여 수량 |
| data.products[].description | String | 상품 상세 설명 |

**성공 응답 예시:**
```json
{
  "code": 200,
  "message": "상품 목록 조회 성공",
  "data": {
    "products": [
      {
        "id": 1,
        "name": "새우깡",
        "price": 5000,
        "stock": 10,
        "description": "손이 가는 새우깡"
      },
      {
        "id": 2,
        "name": "메론킥",
        "price": 7000,
        "stock": 30,
        "description": "맛도리"
      },
      {
        "id": 3,
        "name": "고무장갑",
        "price": 50000,
        "stock": 100,
        "description": "최고급 고무장갑"
      }
    ]
  }
}
```
</details>

### ✅ 상품 단건 조회
사용자에게 상품의 상세 정보를 조회한다.
- Method: `GET`
- Endpoint: `/api/v1/products/{productId}`
<details markdown="1">
<summary>상세 보기</summary>

### **Request**
**Path parameters**

| Field      | Type   | Description | Constraints |
|------------|--------|-------------|-------------|
| productId  | Number | 상품 ID      | 양의 정수   |

### **Response**

| Field   | Type   | Description       |
|---------|--------|-------------------|
| code    | Number | 응답 코드  |
| message | String | 응답 메시지 |
| data | Object | 상품 상세 정보 |
| data.id | Number | 상품 ID |
| data.name | String | 상품 이름 |
| data.price | Number | 상품 가격 |
| data.stock | Number | 상품 잔여 수량 |
| data.description | String | 상품 상세 설명 |

**성공 응답 예시:**
```json
{
  "code": 200,
  "message": "상품 조회 성공",
  "data": {
    "id": 1,
    "name": "새우깡",
    "price": 5000,
    "stock": 10,
    "description": "맛있는 새우깡"
  }
}
```

**에러 응답 예시:**
```json
{
  "code": "PRODUCT_NOT_FOUND",
  "message": "상품이 존재하지 않습니다."
}
```
</details>


## 3️⃣ 주문 (Order)
### ✅ 주문
사용자가 상품을 주문한다.

- Method: `POST`
- Endpoint: `/api/v1/orders`

<details markdown="1">
<summary>상세 보기</summary>

### **Request**

**Request Body**

| Body | Type   | Description | Constraints |
|------|--------|-------------|-------------|
| userId | Number | 사용자 ID      | 양의 정수   |
| userCouponId | Number | 사용가 사용하고자 하는 쿠폰 ID      | 양의 정수 (미사용 시 null)  |
| orderProducts | Array  | 주문할 상품 목록 |
| orderProducts[].productId | Number | 주문할 상품 ID      | 양의 정수   |
| orderProducts[].quantity | Number | 주문 수량      | 양의 정수   |

### **Response**

| Field     | Type   | Description       |
|-----------|--------|-------------------|
| code     | Number | 응답 코드  |
| message  | String | 응답 메시지 |
| data.orderId | Number | 생성된 주문 ID |

**성공 응답 예시:**
```json
{
  "code": 200,
  "message": "주문 생성 성공",
  "data": {
    "orderId": 12345
  }
}
```

**에러 응답 예시:**
```json
{
  "code": "ORDER_PRODUCT_EMPTY",
  "message": "주문 상품이 비어있습니다."
}
```

```json
{
  "code": "USER_NOT_FOUND",
  "message": "사용자를 찾을 수 없습니다."
}
```

```json
{
  "code": "PRODUCT_NOT_FOUND",
  "message": "상품이 존재하지 않습니다."
}
```

```json
{
  "code": "OUT_OF_STOCK_PRODUCT",
  "message": "상품 재고가 부족합니다."
}
```

```json
{
  "code": "NOT_ENOUGH_POINT",
  "message": "포인트가 부족합니다."
}
```

```json
{
  "code": "ALREADY_USED_COUPON",
  "message": "이미 사용된 쿠폰입니다."
}
```

```json
{
  "code": "NOT_OWNED_USER_COUPON",
  "message": "소유하지 않은 쿠폰입니다."
}
```

```json
{
  "code": "EXPIRED_COUPON",
  "message": "쿠폰이 만료되었습니다."
}
```

```json
{
  "code": "PAYMENT_FAILED",
  "message": "결제에 실패했습니다."
}
```
</details>

## 4️⃣ 쿠폰 (Coupon)
### ✅ 보유 쿠폰 목록 조회
사용자가 보유한 쿠폰 목록을 조회한다.

- Method: `GET`
- Endpoint: `/api/v1/users/{userId}/coupons`

<details markdown="1">
<summary>상세 보기</summary>

### **Request**
**Path parameters**

| Field  | Type   | Description | Constraints |
|--------|--------|-------------|-------------|
| userId | Number | 사용자 ID      | 양의 정수   |

### **Response**

| Field                        | Type   | Description                        |
|------------------------------|--------|------------------------------------|
| code                         | Number | 응답 코드                              |
| message                      | String | 응답 메시지                             |
| data.coupons                 | Array  | 쿠폰 목록                              |
| data.coupons[].id            | Number | 쿠폰 ID                              |
| data.coupons[].title         | String | 쿠폰명                                |
| data.coupons[].discountType  | String | 쿠폰 할인 타입(AMOUNT: 정액 / PERCENT: 정률) |
| data.coupons[].discountValue | Number | 쿠폰 할인 금액 또는 비율 (정액: 원 단위, 정률: 백분율) |
| data.coupons[].expiredAt     | String | 쿠폰 만료일 (ISO 8601 형식) |

**성공 응답 예시:**
```json
{
  "code": 200,
  "message": "쿠폰 목록 조회 성공",
  "data": {
    "coupons": [
      {
        "id": 1,
        "title": "여름 맞이 10% 할인 쿠폰",
        "discountType": "PERCENT",
        "discountValue": 10,
        "expiredAt": "2024-12-31T23:59:59Z"
      },
      {
        "id": 2,
        "title": "학생 대상 5,000원 할인 쿠폰",
        "discountType": "AMOUNT",
        "discountValue": 5000,
        "expiredAt": "2024-11-30T23:59:59Z"
      }
    ]
  }
}
```

**에러 응답 예시:**
```json
{
  "code": "USER_NOT_FOUND",
  "message": "사용자를 찾을 수 없습니다."
}
```
</details>

### ✅ 쿠폰 발급
사용자가 선착순으로 쿠폰을 발급받는다.

- Method: `POST`
- Endpoint: `/api/v1/coupons`

<details markdown="1">
<summary>상세 보기</summary>

### **Request**
**Request Body**

| Body   | Type   | Description | Constraints |
|--------|--------|-------------|-------------|
| userId | Number | 사용자 ID      | 양의 정수   |
| couponId | Number | 발급받을 쿠폰 ID      | 양의 정수   |

### **Response**

| Field     | Type   | Description       |
|-----------|--------|-------------------|
| code     | Number | 응답 코드  |
| message  | String | 응답 메시지 |
| data.couponId | Number | 발급된 쿠폰 ID |

**성공 응답 예시:**
```json
{
  "code": 200,
  "message": "쿠폰 발급 성공",
  "data": {
    "couponId": 1
  }
}
```

**에러 응답 예시:**
```json
{
  "code": "USER_NOT_FOUND",
  "message": "사용자를 찾을 수 없습니다."
}
```

```json
{
  "code": "COUPON_NOT_FOUND",
  "message": "쿠폰을 찾을 수 없습니다."
}
```

```json
{
  "code": "OUT_OF_STOCK_COUPON",
  "message": "쿠폰 재고가 부족합니다."
}
```

```json
{
  "code": "ALREADY_APPLIED_COUPON",
  "message": "이미 적용된 쿠폰입니다."
}
```
</details>


## 5️⃣ 상위 상품 (Bestseller)
### ✅ 상위 상품 목록 조회
판매량이 가장 높은 상품 목록을 조회한다.

- Method: `GET`
- Endpoint: `/api/v1/bestsellers`

<details markdown="1">
<summary>상세 보기</summary>

### **Response**

| Field                      | Type   | Description       |
|----------------------------|--------|-------------------|
| code                       | Number | 응답 코드  |
| message                    | String | 응답 메시지 |
| data.bestsellers           | Array  | 상위 상품 목록 |
| data.bestsellers[].id      | Number | 상품 ID |
| data.bestsellers[].name    | String | 상품 이름 |
| data.bestsellers[].price   | Number | 상품 가격 |
| data.bestsellers[].stock   | Number | 상품 잔여 수량 |
| data.bestsellers[].ranking | Number | 상위 상품 순위 (1~5) |

**성공 응답 예시:**
```json
{
  "code": 200,
  "message": "상위 상품 목록 조회 성공",
  "data": {
    "bestsellers": [
      {
        "id": 1,
        "name": "상품 A",
        "price": 10000,
        "stock": 50,
        "ranking": 1
      },
      {
        "id": 2,
        "name": "상품 B",
        "price": 20000,
        "stock": 30,
        "ranking": 2
      }
    ]
  }
}
```

**에러 응답 예시:**
```json
{
  "code": "BESTSELLER_NOT_FOUND",
  "message": "베스트셀러를 찾을 수 없습니다."
}
```
</details>
