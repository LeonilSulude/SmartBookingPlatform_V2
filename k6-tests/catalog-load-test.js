import http from 'k6/http';
import { check, sleep } from 'k6';

/*
CATALOG LOAD TEST

Goal:
Measure performance of retrieving service offers.

Strategy:
Each virtual user logs in once and repeatedly
requests the catalog endpoint through the API Gateway.
*/

export const options = {
  vus: 20,
  duration: '30s',
};

let userEmail;
let password = "securePassword123";
let token;

export default function () {

  // Register once per VU
  if (!userEmail) {

    const uniqueId = __VU + "-" + Date.now();
    userEmail = `catalog${uniqueId}@example.com`;

    http.post(
      "http://localhost:8080/api/auth/register",
      JSON.stringify({
        name: "Catalog Load User",
        email: userEmail,
        password: password,
        role: "PROVIDER"
      }),
      { headers: { "Content-Type": "application/json" } }
    );
  }

  // Login
  const loginRes = http.post(
    "http://localhost:8080/api/auth/login",
    JSON.stringify({
      email: userEmail,
      password: password
    }),
    { headers: { "Content-Type": "application/json" } }
  );

  const body = JSON.parse(loginRes.body);
  token = body.token;

  const headers = {
    headers: {
      "Authorization": `Bearer ${token}`
    }
  };

  // Call catalog
  const res = http.get(
    "http://localhost:8080/api/offers",
    headers
  );

  check(res, {
    "status is 200": (r) => r.status === 200,
    "response time < 500ms": (r) => r.timings.duration < 500,
  });

  sleep(1);
}