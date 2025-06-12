# API 명세서

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

example:
```json
{
  "code": 200,
  "message": "포인트 조회 성공",
  "data": {
    "point": 10000
  }
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

example:
```json
{
  "code": 200,
  "message": "포인트 충전 성공",
  "data": {
    "point": 1300000
  }
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

### **Rsponse**
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

example:
```json
{
  "code": 200,
  "message": "상품 목록 조회 성공",
  "data": [
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

example:
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

example:
```json
{
  "code": 200,
  "message": "주문 생성 성공",
  "data": {
    "orderId": 12345
  }
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

example:
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

example:
```json
{
  "code": 200,
  "message": "쿠폰 발급 성공",
  "data": {
    "couponId": 1
  }
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

| Field     | Type   | Description       |
|-----------|--------|-------------------|
| code     | Number | 응답 코드  |
| message  | String | 응답 메시지 |
| data.bestsellers | Array  | 상위 상품 목록 |
| data.bestsellers[].id | Number | 상품 ID |
| data.bestsellers[].name | String | 상품 이름 |
| data.bestsellers[].price | Number | 상품 가격 |
| data.bestsellers[].stock | Number | 상품 잔여 수량 |
| data.bestsellers[].rank | Number | 상위 상품 순위 (1~5) |

example:
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
        "rank": 1
      },
      {
        "id": 2,
        "name": "상품 B",
        "price": 20000,
        "stock": 30,
        "rank": 2
      }
    ]
  }
}
```

</details>
