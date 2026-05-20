// ============================================================
// k6 load test — POST /api/auth/register
// ============================================================
// Why k6 instead of `hey`:
//   hey sends the same body to all N requests. /register requires
//   unique email + username (DB unique constraint), so only the
//   first request would succeed — invalidating the test.
//   k6 lets us generate a fresh body per iteration.
//
// Run:
//   k6 run scripts/loadtest-register.js
//
// Tune via env vars:
//   VUS=25 DURATION=60s k6 run scripts/loadtest-register.js
//
// Cleanup after (Supabase SQL editor):
//   DELETE FROM buruhs   WHERE id IN (SELECT id FROM users WHERE email LIKE 'loadtest-%');
//   DELETE FROM mandors  WHERE id IN (SELECT id FROM users WHERE email LIKE 'loadtest-%');
//   DELETE FROM supirs   WHERE id IN (SELECT id FROM users WHERE email LIKE 'loadtest-%');
//   DELETE FROM refresh_tokens WHERE user_id IN (SELECT id FROM users WHERE email LIKE 'loadtest-%');
//   DELETE FROM users    WHERE email LIKE 'loadtest-%';
// ============================================================

import http from 'k6/http';
import { check } from 'k6';

// VUS = Virtual Users (kira-kira setara dengan concurrent client).
// 25 VUs adalah titik mulai yang aman — NFR target 50 RPS, jadi
// kita target kira-kira 2× capacity baseline untuk lihat headroom.
const VUS = parseInt(__ENV.VUS || '25', 10);
const DURATION = __ENV.DURATION || '60s';

export const options = {
  vus: VUS,
  duration: DURATION,

  // Thresholds = pass/fail criteria. k6 akan exit code 99 kalau gagal.
  // Disesuaikan dengan NFR write endpoint:
  //   - P95 ≤ 1200 ms
  //   - Error rate < 1%
  thresholds: {
    http_req_duration: ['p(95)<1200'],
    http_req_failed:   ['rate<0.01'],
  },

  // Summary tampilkan persentil yang penting untuk profiling.
  summaryTrendStats: ['min', 'avg', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
};

// Helper: bikin ID super unik supaya tidak bentrok antara VU,
// iterasi, dan even antara run yang berdekatan.
function uniqueId() {
  // __VU  = ID virtual user (1..VUS)
  // __ITER = nomor iterasi VU itu (mulai dari 0)
  return `${__VU}-${__ITER}-${Date.now()}`;
}

export default function () {
  const id = uniqueId();

  const payload = JSON.stringify({
    username: `loadtest-${id}`,
    email:    `loadtest-${id}@example.com`,
    password: 'Password123!',
    role:     'BURUH',          // default-allowed self-registration role
  });

  const params = {
    headers: { 'Content-Type': 'application/json' },
    // Tag setiap request supaya kelihatan di Prometheus
    // sebagai uri="/api/auth/register" (Spring Boot otomatis tag berdasarkan path).
    tags: { name: 'register' },
  };

  const res = http.post(
    'http://localhost:8081/api/auth/register',
    payload,
    params
  );

  // 201 Created = sukses register. Selain itu = anomaly.
  check(res, {
    'status is 201': (r) => r.status === 201,
  });
}
