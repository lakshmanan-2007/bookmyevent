# BookMyEvent — High-Level Design (HLD)

> How the system is built today, and how it scales to millions of users. This is the
> "system design" story for interviews. See `architecture.svg` for the diagram.

## 1. Problem statement

An event ticket-booking platform where thousands of users may try to book the **same event's last
seats at the same time**. The core requirement: **never oversell** — sell at most `availableSeats`,
even under heavy concurrent load — while staying fast for the read-heavy "browse events" path.

## 2. Current architecture (deployed)

```
Browser → Vercel (Next.js frontend) → Render (Spring Boot API) → Supabase (PostgreSQL)
```

- **Frontend** — Next.js 14 + TypeScript on Vercel (global CDN, instant static delivery).
- **Backend** — Spring Boot 3 REST API on Render (Dockerized). Stateless (JWT), so it can be
  replicated horizontally.
- **Database** — PostgreSQL on Supabase (session-pooler connection).
- **Caching** — Caffeine in-memory cache for the event catalog (read-heavy).
- **Correctness** — booking runs in a transaction that takes a **DB pessimistic lock**
  (`SELECT ... FOR UPDATE`) on the event row, so concurrent bookings are serialised.
- **Rate limiting** — in-memory fixed-window limiter (100 req/min per IP) protects the API.

This is correct and cheap for a **single backend instance**.

## 3. Scaling to 1,000,000 users — what changes

When traffic grows, we run **many backend instances behind a load balancer**. A single-instance
DB lock is still correct (the lock lives in Postgres, shared by all instances), but the DB can
become the bottleneck and a per-instance in-memory rate limiter is no longer global. So:

| Concern | Single instance (today) | At scale (design) |
|---|---|---|
| **No oversell** | DB pessimistic lock | **Redis distributed lock** (`app.lock.provider=redis`) per event + DB lock as the final guard |
| **Event catalog reads** | Caffeine (per instance) | **Redis** shared cache (or CDN for the JSON) |
| **Rate limiting** | In-memory per instance | **Redis-backed** counter (global across instances) |
| **Traffic spikes / sellouts** | Direct DB writes | **Queue** (Kafka/SQS) — accept booking requests, process serially per event |
| **DB load** | Single Postgres | **Read replicas** for reads, primary for writes; connection pooling (PgBouncer) |
| **Frontend delivery** | Vercel | Vercel CDN (already global) |
| **Sessions** | Stateless JWT | Stateless JWT (no change — scales freely) |

### Scaled diagram

```
                         ┌─────────────┐
        Users  ───────▶  │   CDN /     │  (static frontend — Vercel)
                         │  Vercel     │
                         └─────┬───────┘
                               │  API calls (JWT)
                         ┌─────▼───────┐
                         │   Load      │
                         │  Balancer   │
                         └─────┬───────┘
              ┌────────────────┼────────────────┐
        ┌─────▼────┐     ┌─────▼────┐      ┌─────▼────┐
        │ API #1   │     │ API #2   │  ... │ API #N   │   (stateless Spring Boot)
        └─────┬────┘     └─────┬────┘      └─────┬────┘
              │  Redis lock + rate-limit + cache │
              └───────────────┬──────────────────┘
                        ┌──────▼──────┐
                        │    Redis    │  (distributed lock, cache, counters)
                        └─────────────┘
              write │                       │ async
              ┌─────▼──────┐        ┌───────▼───────┐
              │ PostgreSQL │◀──read─│ Read replicas │
              │  (primary) │        └───────────────┘
              └────────────┘
```

## 4. The booking flow (oversell-safe, at scale)

1. Request hits the **rate limiter** (global, Redis-backed) — abusive clients get `429`.
2. API acquires a **Redis distributed lock** on `event:{id}` (`SET key val NX PX 10s`).
   Only one instance holds it at a time.
3. Inside a **DB transaction**, it re-reads the event with a pessimistic lock, checks
   `availableSeats >= quantity`, decrements, saves the booking, commits.
4. Lock released. Next waiter proceeds.

**Two layers of safety:** the Redis lock serialises across instances; the DB lock is the final
guarantee inside the transaction. Even if Redis fails, the DB lock still prevents overselling.

## 5. Bottlenecks & trade-offs

- **Hot event = single lock = serial bookings.** Trade-off of strong correctness. Mitigation:
  shard seats into buckets, or use an atomic `UPDATE ... WHERE available_seats >= :qty` (optimistic,
  lock-free) for very high throughput.
- **Pessimistic vs optimistic locking:** pessimistic = predictable under high contention (fewer
  retries); optimistic = better throughput under low contention. We chose pessimistic because a
  sellout is inherently high-contention.
- **Cache invalidation:** every write (`book`/`cancel`/event update) evicts the events cache so
  reads never serve stale seat counts.
- **CAP:** we favour **consistency** for booking (never oversell) over availability of that single
  write path; reads stay highly available via cache/replicas.

## 6. Observability & reliability

- **Actuator** exposes `/actuator/health` and `/actuator/metrics`.
- **Load tested** with k6 (`loadtest/booking-loadtest.js`) — validates no oversell under load.
- **CI/CD:** push to `main` → Render + Vercel auto-deploy.

## 7. One-line summary (interview)

> "Stateless Spring Boot API behind a load balancer; oversell-safety via a Redis distributed lock
> plus a DB pessimistic lock; Redis for shared cache and global rate limiting; Postgres primary
> with read replicas; Next.js on a CDN. Favours consistency on the booking write path, high
> availability on reads."
