import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';

const TEST_TYPE = __ENV.TEST_TYPE || 'smoke';
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const COUPON_ENDPOINT = '/api/v1/coupons';

const successRate = new Rate('success_rate');
const failCount = new Counter('fail_count');
const requestDuration = new Trend('request_duration');

export let options = {};

switch (TEST_TYPE) {
    case 'smoke':
        options = {
            vus: 100,
            duration: '10s',
        };
        break;

    case 'load':
        options = {
            stages: [
                { duration: '2m', target: 500 },
                { duration: '2m', target: 1000 },
                { duration: '3m', target: 1500 },
                { duration: '2m', target: 1000 },
                { duration: '1m', target: 0 },
            ],
            thresholds: {
                http_req_duration: ['p(95)<2000'],
                http_req_failed: ['rate<0.15'],
                'success_rate': ['rate>0.85'],
            },
        };
        break;

    case 'stress':
        options = {
            stages: [
                { duration: '1m', target: 1000 },
                { duration: '1m', target: 1000 },
                { duration: '1m', target: 2000 },
                { duration: '1m', target: 2000 },
                { duration: '1m', target: 3000 },
                { duration: '1m', target: 3000 },
                { duration: '1m', target: 4000 },
                { duration: '1m', target: 4000 },
                { duration: '1m', target: 5000 },
                { duration: '1m', target: 5000 },
                { duration: '30s', target: 0 },
            ],
            thresholds: {
                http_req_duration: ['p(95)<1000'],
                http_req_failed: ['rate<0.15'],
                'success_rate': ['rate>0.85'],
            },
        };
        break;

    case 'peak':
        options = {
            stages: [
                { duration: '10s', target: 100 },
                { duration: '10s', target: 100 },
                { duration: '20s', target: 2000 },
                { duration: '10s', target: 100 },
                { duration: '20s', target: 0 },
            ],
            thresholds: {
                http_req_duration: ['p(95)<1000'],
                http_req_failed: ['rate<0.15'],
                'success_rate': ['rate>0.85'],
            },
        };
        break;

    default:
        console.error(`Unknown TEST_TYPE: ${TEST_TYPE}`);
        break;
}

export default function () {
    const userId = __VU * 100000 + __ITER + 1;
    const couponId = Math.floor(Math.random() * 6) + 8;
    const url = `${BASE_URL}${COUPON_ENDPOINT}`;
    const payload = JSON.stringify({
        "userId": userId,
        "couponId": couponId
    });

    const headers = { 'Content-Type': 'application/json' };
    const res = http.post(url, payload, { headers });

    const success = check(res, {
        'status is 200': (r) => r.status === 200,
        'total request': (r) => [200, 409].includes(r.status),
    });

    successRate.add(success);
    if (!success) failCount.add(1);
    requestDuration.add(res.timings.duration);

    sleep(1);
}