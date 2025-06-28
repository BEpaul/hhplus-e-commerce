import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';

// 커스텀 메트릭 정의
const errorRate = new Rate('errors');
const responseTimeTrend = new Trend('response_time_trend');
const throughputCounter = new Counter('total_requests');

// 테스트 설정
export const options = {
  stages: [
    { duration: '10s', target: 50 },
    { duration: '20s', target: 300 },
    { duration: '30s', target: 600 },
    { duration: '30s', target: 1000 },
    { duration: '20s', target: 1000 },
    { duration: '15s', target: 500 },
    { duration: '10s', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(50)<300', 'p(95)<800', 'p(99)<1500'],
    http_req_failed: ['rate<0.05'],
    http_reqs: ['rate>100'],
    'response_time_trend': ['p(50)<150', 'p(95)<500'],
    'errors': ['rate<0.05'],
    http_req_waiting: ['p(95)<600'],
    http_req_connecting: ['p(95)<100'],
    http_req_duration: ['p(99.9)<2000'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export default function () {
  const startTime = Date.now();
  
  const response = http.get(`${BASE_URL}/api/v1/bestsellers`, {
    headers: {
      'Accept': 'application/json',
      'User-Agent': 'k6-load-test',
    },
  });
  
  const responseTime = Date.now() - startTime;
  responseTimeTrend.add(responseTime);
  throughputCounter.add(1);
  
  const success = check(response, {
    'status is 200': (r) => r.status === 200,
    'response time < 500ms': (r) => r.timings.duration < 500,
    'response has data': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.code === 200 && body.data && Array.isArray(body.data);
      } catch (e) {
        return false;
      }
    },
    'response structure is correct': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.hasOwnProperty('code') && 
               body.hasOwnProperty('data') && 
               body.hasOwnProperty('message');
      } catch (e) {
        return false;
      }
    },
    'response size is reasonable': (r) => r.body.length > 0 && r.body.length < 10000,
  });

  errorRate.add(!success);

  if (response.status !== 200) {
    console.log(`Error: ${response.status} - ${response.body}`);
  }

  const userType = __VU % 5;
  let sleepTime;
  
  if (userType === 0) {
    sleepTime = Math.random() * 0.8 + 0.1;
  } else if (userType === 1) {
    sleepTime = Math.random() * 1.5 + 0.3;
  } else if (userType === 2) {
    sleepTime = Math.random() * 2 + 0.5;
  } else if (userType === 3) {
    sleepTime = Math.random() * 3 + 1;
  } else {
    sleepTime = Math.random() * 5 + 2;
  }
  
  if (Math.random() < 0.1) {
    sleepTime = Math.random() * 0.5 + 0.05;
  }
  
  sleep(sleepTime);
}
