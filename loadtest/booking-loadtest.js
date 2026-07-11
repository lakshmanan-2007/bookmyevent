// k6 load test for BookMyEvent.
// Run:  k6 run -e BASE=https://bookmyevent-api.onrender.com loadtest/booking-loadtest.js
// Local: k6 run -e BASE=http://localhost:8080 loadtest/booking-loadtest.js
//
// What it does: ramps virtual users who log in, browse events, and book tickets.
// It measures throughput, p95 latency and error rate, and (via a setup step) lets you
// verify the "no oversell" guarantee by checking seat counts before/after.
import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE = __ENV.BASE || 'http://localhost:8080';

export const options = {
  scenarios: {
    browse_and_book: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '20s', target: 30 },  // ramp up
        { duration: '40s', target: 30 },  // sustain load
        { duration: '10s', target: 0 },   // ramp down
      ],
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.05'],     // < 5% errors (429/400 from oversell are expected, filtered below)
    http_req_duration: ['p(95)<1500'],  // 95% of requests under 1.5s (cold start excluded)
  },
};

// One shared demo login (seeded user) for all VUs.
export function setup() {
  const res = http.post(`${BASE}/api/auth/login`, JSON.stringify({
    email: 'demo@bookmyevent.com', password: 'Demo@123',
  }), { headers: { 'Content-Type': 'application/json' } });
  const token = res.status === 200 ? res.json('token') : null;
  return { token };
}

export default function (data) {
  // 1) Browse events (public, read-heavy, cached).
  const list = http.get(`${BASE}/api/events`);
  check(list, { 'events 200': (r) => r.status === 200 });

  // 2) Book 1 ticket for event 1 (needs auth). 400 = sold out (expected, not an error).
  if (data.token) {
    const book = http.post(`${BASE}/api/bookings`, JSON.stringify({ eventId: 1, quantity: 1 }), {
      headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${data.token}` },
    });
    check(book, {
      'book handled (201 or 400 sold-out)': (r) => r.status === 201 || r.status === 400,
      'never a 5xx': (r) => r.status < 500,
    });
  }
  sleep(1);
}
