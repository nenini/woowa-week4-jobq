import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    thresholds: {
        http_req_failed: ['rate<0.01'],
        http_req_duration: ['p(95)<800'],
    },
    stages: [
        { duration: '30s', target: 20 }, // 0 -> 20
        { duration: '2m',  target: 20 }, // 유지
        { duration: '30s', target: 40 }, // 20 -> 40
        { duration: '2m',  target: 40 }, // 유지
    ],
};

const url = 'http://localhost:8080/jobs/email_welcome';

export default function () {
    const payload = JSON.stringify({ userId: Math.floor(Math.random()*100000) });
    const headers = { 'Content-Type': 'application/json' };
    const res = http.post(url, payload, { headers });
    check(res, { 'status 200': r => r.status === 200 });
    sleep(0.2);
}
