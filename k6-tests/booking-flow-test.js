import http from 'k6/http';
import { check, sleep } from 'k6';

/*
BOOKING FLOW LOAD TEST

Simulates a realistic provider workflow:

1) Register provider
2) Login to obtain JWT
3) Create service offer
4) Create resource linked to that offer
5) Attempt to create bookings

Multiple virtual users may attempt overlapping booking times.
If the system correctly prevents double booking it will return HTTP 409.
Therefore both 201 (created) and 409 (conflict) are considered valid outcomes.
*/

export const options = {
  vus: 5,
  duration: '30s',
};

let email;
let password = "securePassword123";
let token;
let offerId;
let resourceId;

export default function () {

  /*
  Register once per Virtual User.
  Each VU generates a unique email to avoid registration conflicts.
  */
  if (!email) {

    const uniqueId = `${__VU}-${Date.now()}`;
    email = `provider${uniqueId}@example.com`;

    http.post(
      "http://localhost:8080/api/auth/register",
      JSON.stringify({
        name: "Load Test Provider",
        email: email,
        password: password,
        role: "PROVIDER"
      }),
      { headers: { "Content-Type": "application/json" } }
    );
  }

  /*
  Authenticate and obtain JWT token
  */
  const loginRes = http.post(
    "http://localhost:8080/api/auth/login",
    JSON.stringify({
      email: email,
      password: password
    }),
    { headers: { "Content-Type": "application/json" } }
  );

  token = JSON.parse(loginRes.body).token;

  const headers = {
    headers: {
      "Content-Type": "application/json",
      "Authorization": `Bearer ${token}`
    }
  };

  /*
  Create offer once per Virtual User
  */
  if (!offerId) {

    const offerRes = http.post(
      "http://localhost:8080/api/offers",
      JSON.stringify({
        title: "Load Test Offer",
        description: "Offer created during load testing",
        category: "OTHER",
        providerName: "LoadTest Inc",
        location: "Lisbon"
      }),
      headers
    );

    offerId = JSON.parse(offerRes.body).id;
  }

  /*
  Create resource linked to the offer
  */
  if (!resourceId) {

    const resourceRes = http.post(
      "http://localhost:8080/api/resources",
      JSON.stringify({
        offerId: offerId,
        name: `Resource-${__VU}`,
        active: true,
        price: 150,
        durationInMinutes: 60
      }),
      headers
    );

    resourceId = JSON.parse(resourceRes.body).id;
  }

  /*
  Attempt booking.

  Each VU books at a different time slot (spread by 1 hour)
  to reduce conflicts, although some overlap may still occur
  depending on execution timing.
  */
  const start = new Date(Date.now() + (__VU * 3600000));
  const end = new Date(start.getTime() + 3600000);

  const bookingRes = http.post(
    "http://localhost:8080/api/bookings",
    JSON.stringify({
      resourceId: resourceId,
      customerName: "Load Test Customer",
      customerEmail: email,
      startTime: start.toISOString(),
      endTime: end.toISOString()
    }),
    headers
  );

  /*
  Booking is considered correctly handled if:
  - 201 → booking created
  - 409 → booking rejected due to schedule conflict
  */
  check(bookingRes, {
    "booking request handled correctly": (r) =>
      r.status === 201 || r.status === 409
  });

  sleep(1);
}