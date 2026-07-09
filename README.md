# BookMyEvent

A full-stack event ticket booking application. Users browse events, book tickets, and manage their bookings; admins manage the event catalog. The interesting part is the booking path: it is **oversell-safe under concurrent load**, guarded by a database pessimistic lock.

**Stack:** Java 17 · Spring Boot 3 · Spring Security (JWT) · Spring Data JPA · PostgreSQL / H2 · Caffeine cache · Next.js 14 · TypeScript · React

---

## Demo

The app runs locally — backend on `:8080`, frontend on `:3000` (see [Running it](#running-it)). It has been verified end to end: `mvn test` passes (including a 30-thread concurrency test), the Next.js build compiles cleanly, and data persists in PostgreSQL across restarts.

**Demo accounts** (seeded automatically on first run):

| Role  | Email                 | Password  |
|-------|-----------------------|-----------|
| Admin | admin@bookmyevent.com | Admin@123 |
| User  | demo@bookmyevent.com  | Demo@123  |

> Tip: drop screenshots (home page, booking flow, Swagger UI) into a `screenshots/` folder and embed them here to make the README stand out.

---

## Why this project

It is built to demonstrate the things interviewers actually probe in a backend round:

- **Layered architecture** — `Controller → Service → Repository`, DTOs at the boundary, entities never leak out.
- **Security** — stateless JWT auth, BCrypt password hashing, role-based authorization (`USER` / `ADMIN`).
- **Concurrency** — the classic "two people book the last seat" race, solved with a pessimistic write lock (`SELECT ... FOR UPDATE`) and proven by a multi-threaded test.
- **Caching** — the read-heavy event catalog is cached with Caffeine and evicted on writes.
- **Validation & error handling** — bean validation on every request, one uniform JSON error shape for the whole API.
- **Testing** — unit tests (Mockito) for booking rules + an integration test that races 30 threads for 5 seats.
- **Deployability** — multi-stage Dockerfile and a `docker-compose` that brings up Postgres + the API.

---

## Architecture

```
Next.js (TypeScript)  ──HTTP / JWT──▶  Spring Boot REST API  ──JPA──▶  PostgreSQL / H2
      │                                       │
   AuthContext                          Pessimistic lock on Event row
   fetch API client                     Caffeine cache on event catalog
```

Request flow for a booking:

```
POST /api/bookings                      (JWT verified by JwtAuthenticationFilter)
   → BookingController                  (extracts userId from the token principal)
   → BookingService.book()              (@Transactional)
        → EventRepository.findByIdForUpdate()   -- SELECT ... FOR UPDATE (row lock)
        → check availableSeats, decrement, save booking
   → 201 Created + BookingResponse
```

Because the event row is locked for the duration of the transaction, any other
booking for the same event waits its turn — so the seat count can never go
negative, no matter how many requests arrive at once.

---

## Project layout

```
BookMyEvent/
├── backend/                     Spring Boot API
│   ├── src/main/java/com/lakshmanan/bookmyevent/
│   │   ├── domain/              User, Event, Booking entities + enums
│   │   ├── repository/          JPA repositories (incl. pessimistic-lock query)
│   │   ├── dto/                 Request/response records with validation
│   │   ├── service/             AuthService, EventService, BookingService
│   │   ├── security/            JWT service, auth filter, UserPrincipal
│   │   ├── web/                 REST controllers + global exception handler
│   │   └── config/              Security, cache, dev data seeder
│   ├── src/test/…               Unit test + concurrency test
│   └── Dockerfile
├── frontend/                    Next.js + TypeScript client
│   └── src/
│       ├── app/                 Pages: events, login, register, bookings
│       ├── components/          Navbar, EventCard
│       └── lib/                 API client, auth context, shared types
└── docker-compose.yml
```

---

## Running it

### Backend (Java 17+ required)

```bash
cd backend
mvn spring-boot:run          # requires Maven 3.9+ and JDK 17+
```

Starts on **http://localhost:8080** with an in-memory H2 database and demo data
(dev profile is active by default — zero setup needed).

- Swagger UI: http://localhost:8080/swagger-ui.html
- H2 console: http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:bookmyevent`)

**Demo logins** (seeded on startup):

| Role  | Email                    | Password   |
|-------|--------------------------|------------|
| Admin | admin@bookmyevent.com    | Admin@123  |
| User  | demo@bookmyevent.com     | Demo@123   |

Run the tests (includes the concurrency test):

```bash
cd backend
mvn test
```

### Frontend (Node 18+ required)

```bash
cd frontend
cp .env.local.example .env.local     # NEXT_PUBLIC_API_URL=http://localhost:8080
npm install
npm run dev
```

Open **http://localhost:3000**.

### Everything with Docker (Postgres + API)

```bash
docker compose up --build
```

The API runs on the `prod` profile against PostgreSQL. Run the frontend
separately with `npm run dev`.

---

## API reference

| Method | Endpoint                     | Auth        | Description                        |
|--------|------------------------------|-------------|------------------------------------|
| POST   | `/api/auth/register`         | public      | Create account, returns JWT        |
| POST   | `/api/auth/login`            | public      | Log in, returns JWT                |
| GET    | `/api/events`                | public      | List events (paginated)            |
| GET    | `/api/events/{id}`           | public      | Event detail (cached)              |
| POST   | `/api/events`                | ADMIN       | Create event                       |
| PUT    | `/api/events/{id}`           | ADMIN       | Update event                       |
| DELETE | `/api/events/{id}`           | ADMIN       | Delete event                       |
| POST   | `/api/bookings`              | USER        | Book tickets (oversell-safe)       |
| GET    | `/api/bookings`              | USER        | My bookings (paginated)            |
| POST   | `/api/bookings/{id}/cancel`  | USER        | Cancel a booking, restores seats   |

Example:

```bash
# login
curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"demo@bookmyevent.com","password":"Demo@123"}'

# book 2 tickets for event 1
curl -s -X POST http://localhost:8080/api/bookings \
  -H "Authorization: Bearer <TOKEN>" \
  -H 'Content-Type: application/json' \
  -d '{"eventId":1,"quantity":2}'
```

---

## Talking points for an interview

- **How do you stop overselling?** Pessimistic write lock on the event row inside the
  booking transaction — see `EventRepository.findByIdForUpdate` and `BookingService.book`.
  Trade-off vs. optimistic locking / a Redis distributed lock is discussed below.
- **Why pessimistic over optimistic here?** Bookings for a hot event are high-contention;
  optimistic locking would cause many retries. For a multi-instance deployment the same
  idea moves to a Redis lock or an atomic `UPDATE ... WHERE available_seats >= :qty`.
- **Why stateless JWT?** No server session store, so the API scales horizontally.
- **Where is the cache and how is it kept correct?** Caffeine caches event reads; every
  write (create/update/delete/book/cancel) evicts it via `@CacheEvict`.

---

## Notes

- Default dev profile uses H2 so the project runs with a single command. Switch to
  PostgreSQL with `SPRING_PROFILES_ACTIVE=prod` (or Docker Compose).
- The JWT secret and DB credentials are read from environment variables in production
  (`APP_JWT_SECRET`, `JDBC_URL`, `DB_USER`, `DB_PASSWORD`).
