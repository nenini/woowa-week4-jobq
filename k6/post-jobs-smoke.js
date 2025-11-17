import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    vus: 10,
    duration: '2m',
    thresholds: {
        http_req_failed: ['rate<0.01'],
        http_req_duration: ['p(95)<800'],
    },
};

export default function () {
    const url = 'http://localhost:8080/jobs/email_welcome';
    const payload = JSON.stringify({ userId: Math.floor(Math.random()*1e9) });
    const headers = { 'Content-Type': 'application/json' };

    const res = http.post(url, payload, { headers });
    check(res, { 'status 200': (r) => r.status === 200 });

    sleep(0.2);
}
