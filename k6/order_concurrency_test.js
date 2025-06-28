import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const API_BASE = '/api/v1';

// 커스텀 메트릭
const successRate = new Rate('success_rate');
const failCount = new Counter('fail_count');
const stockOverflowCount = new Counter('stock_overflow_count');
const duplicateOrderCount = new Counter('duplicate_order_count');
const lockWaitTime = new Trend('lock_wait_time');

// 동시 주문 테스트 설정
export let options = {
    stages: [
        { duration: '1s', target: 5 },     // 1초간 5명으로 증가
        { duration: '1s', target: 15 },    // 1초간 15명으로 증가
        { duration: '1s', target: 0 },     // 1초간 0명으로 감소
    ],
    thresholds: {
        'http_req_duration': ['p(95)<3000'],
        'http_req_failed': ['rate<0.15'],
        'success_rate': ['rate>0.85'],
        'lock_wait_time': ['p(95)<2000'],
    },
};

// 테스트 데이터 - 제한된 재고로 동시성 테스트
const TEST_USERS = Array.from({length: 5}, (_, i) => ({ id: i + 1, point: 1000000 }));
const LIMITED_STOCK_PRODUCT = { id: 4, name: '제한 재고 상품', price: 1, stock: 100000 };

// 동시 주문 테스트
export default function () {
    const user = TEST_USERS[Math.floor(Math.random() * TEST_USERS.length)];
    const quantity = Math.floor(Math.random() * 2) + 1; // 1-2개

    // 동일한 상품에 대한 동시 주문 시도
    const orderData = {
        userId: user.id,
        userCouponId: null,
        orderProducts: [
            {
                productId: LIMITED_STOCK_PRODUCT.id,
                quantity: quantity
            }
        ]
    };

    const payload = JSON.stringify(orderData);
    const params = {
        headers: {
            'Content-Type': 'application/json',
        },
    };

    const startTime = Date.now();
    const response = http.post(`${BASE_URL}${API_BASE}/orders`, payload, params);
    const endTime = Date.now();
    
    lockWaitTime.add(endTime - startTime);

    if (response.status === 200) {
        successRate.add(1);
        console.log(`주문 성공 - 사용자: ${user.id}, 수량: ${quantity}`);
    } else {
        successRate.add(0);
        failCount.add(1);
        
        const responseBody = response.body;
        if (responseBody.includes('재고') || responseBody.includes('stock')) {
            stockOverflowCount.add(1);
            console.log(`재고 부족 - 사용자: ${user.id}, 수량: ${quantity}`);
        } else if (responseBody.includes('중복') || responseBody.includes('duplicate')) {
            duplicateOrderCount.add(1);
            console.log(`중복 주문 감지 - 사용자: ${user.id}`);
        } else {
            console.log(`주문 실패 - 사용자: ${user.id}, 응답 코드: ${response.status} - ${responseBody}`);
        }
    }

    // 짧은 간격으로 연속 요청 (동시성 테스트)
    sleep(0.2);
}

// 테스트 완료 후 요약
export function handleSummary(data) {
    console.log('\n=== 동시 주문 분산락 테스트 결과 ===');
    console.log(`총 요청 수: ${data.metrics.http_reqs.values.count}`);
    console.log(`성공율: ${(data.metrics.success_rate.values.rate * 100).toFixed(2)}%`);
    console.log(`실패 수: ${data.metrics.fail_count.values.count}`);
    console.log(`재고 부족으로 인한 실패: ${data.metrics.stock_overflow_count.values.count}`);
    console.log(`중복 주문 감지: ${data.metrics.duplicate_order_count.values.count}`);
    console.log(`평균 락 대기 시간: ${data.metrics.lock_wait_time.values.avg.toFixed(2)}ms`);
    console.log(`95% 락 대기 시간: ${data.metrics.lock_wait_time.values['p(95)'].toFixed(2)}ms`);
    
    return {
        'concurrent_order_test_summary.json': JSON.stringify(data, null, 2),
    };
}