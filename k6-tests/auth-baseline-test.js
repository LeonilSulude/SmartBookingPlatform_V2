import http from 'k6/http';
import { check, sleep } from 'k6';

/*
AUTH BASELINE TEST

Goal
----
Measure authentication performance through the API Gateway.

Test strategy
-------------
Each Virtual User (VU):
1) Registers once (setup step)
2) Performs repeated login requests (operation under test)

This allows us to measure login latency and stability
without constantly creating new users.
*/

export const options = {
  vus: 10,          // concurrent virtual users
  duration: '30s'   // test duration
};

/*
Persisted per Virtual User.
Each VU keeps its own account to avoid email conflicts.
*/
let userEmail;
let userPassword = "securePassword123";

export default function () {

  /*
  Register the user only once per VU.
  Unique email prevents duplicate registration errors.
  */
  if (!userEmail) {

    const uniqueId = __VU + "-" + Date.now();
    userEmail = `test${uniqueId}@example.com`;

    const registerPayload = JSON.stringify({
      name: "Load Test User",
      email: userEmail,
      password: userPassword,
      role: "PROVIDER"
    });

    const params = {
      headers: {
        "Content-Type": "application/json",
        "Accept": "application/json"
      }
    };

    const registerRes = http.post(
      "http://localhost:8080/api/auth/register",
      registerPayload,
      params
    );

    check(registerRes, {
      "register success": (r) => r.status === 200 || r.status === 201
    });
  }

  /*
  Login request (main operation we want to measure)
  */
  const loginPayload = JSON.stringify({
    email: userEmail,
    password: userPassword
  });

  const params = {
    headers: {
      "Content-Type": "application/json",
      "Accept": "application/json"
    }
  };

  const loginRes = http.post(
    "http://localhost:8080/api/auth/login",
    loginPayload,
    params
  );

  /*
  Validate both correctness and performance.
  */
  check(loginRes, {
    "login status 200": (r) => r.status === 200,
    "login response < 300ms": (r) => r.timings.duration < 300
  });

  /*
  Simulate realistic user behaviour.
  */
  sleep(1);
}