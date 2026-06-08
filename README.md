# Event Ledger

Two Spring Boot microservices that ingest financial transaction events, handle idempotency and out-of-order delivery, and expose observability and resiliency features.

## Architecture

```
Client  -->  Gateway Service (8080, H2)  --REST-->  Account Service (8081, H2)
```

- **Gateway Service** — public API for event submission and retrieval. Validates input, enforces idempotency, propagates trace IDs, calls the Account Service before persisting events locally, and proxies balance queries.
- **Account Service** — internal API for balances and transaction application. Computes balance as `sum(CREDIT) - sum(DEBIT)` regardless of arrival order.

### API contracts

**Gateway (public)**

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/events` | Submit a transaction event (`201` new, `200` duplicate) |
| `GET` | `/events/{id}` | Retrieve a single event (local data; works when Account is down) |
| `GET` | `/events?account={accountId}` | List events for an account, ordered by `eventTimestamp` (local data) |
| `GET` | `/accounts/{accountId}/balance` | Proxy balance query; returns `503` when Account Service is unreachable |
| `GET` | `/health` | Health check with DB connectivity |

**Account Service (internal)**

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/accounts/{accountId}/transactions` | Apply a transaction (`201` new, `200` duplicate) |
| `GET` | `/accounts/{accountId}/balance` | Current balance |
| `GET` | `/accounts/{accountId}` | Account details + recent transactions |
| `GET` | `/health` | Health check with DB connectivity |

### How the core requirements are met

- **Idempotency** — `eventId` is the idempotency key in both services (lookup before write + unique DB constraint). Duplicate submissions return the original record with `200 OK`.
- **Out-of-order tolerance** — events are stored as received; balance is an order-independent `SUM(CREDIT) - SUM(DEBIT)` aggregate, and listings are sorted by `eventTimestamp`.
- **Validation** — missing fields, zero/negative amounts (`@DecimalMin`), and unknown event types (e.g. `"TRANSFER"`) all return a structured RFC 9457 `400` with a meaningful message.
- **Graceful degradation** — when the Account Service is unavailable: `POST /events` returns `503` (no event stored), `GET /events*` still work from local data, and `GET /accounts/{id}/balance` returns a clear `503`.

### Design assumptions

| Assumption | Rationale |
|---|---|
| USD-only currency | Simplifies validation; all amounts in USD |
| Last 10 events/transactions | No pagination requirement in spec |
| No event stored if Account Service fails | Gateway returns `503` and does not persist |
| One DB per service (H2 in-memory) | Per assignment; no HA required |

### Resiliency

The Gateway uses a **Resilience4j circuit breaker** on every call to the Account Service (transaction apply and balance proxy), implemented in `AccountServiceClient` (`gateway-service`) and tuned in `application.yml`. After repeated failures the circuit opens and the Gateway returns `503 Service Unavailable` immediately instead of hanging or returning `500`. A circuit breaker was chosen over plain retries because the failure mode here (Account Service down) is not transient — retrying would only add latency and load, whereas opening the circuit fails fast and protects the Gateway's resources.

The breaker is paired with HTTP **connect/read timeouts** on the client (`ACCOUNT_SERVICE_CONNECT_TIMEOUT` / `ACCOUNT_SERVICE_READ_TIMEOUT`). This guards the *slow*-response case: a read timeout surfaces as a `503` and also counts as a circuit-breaker failure, so a persistently slow Account Service will trip the breaker rather than tying up gateway threads. Resiliency lives on the caller (Gateway) because the Account Service has no downstream dependency of its own.

## Prerequisites

- Java 21
- Maven 3.9+
- Docker & Docker Compose (optional)

## Quick start with Docker Compose

```bash
make docker-build
make docker-up
```

- Gateway: http://localhost:8080
- Account Service: http://localhost:8081 (internal; not intended for external clients)
- Swagger UI: http://localhost:8080/swagger-ui.html

## Local development

```bash
# Terminal 1
cd account-service && mvn spring-boot:run

# Terminal 2
cd gateway-service && mvn spring-boot:run
```

Configure the Account Service URL via environment variable:

```bash
export ACCOUNT_SERVICE_URL=http://localhost:8081
```

## API examples

Submit an event:

```bash
curl -X POST http://localhost:8080/events \
  -H "Content-Type: application/json" \
  -d '{
    "eventId": "evt-001",
    "accountId": "acct-123",
    "type": "CREDIT",
    "amount": 150.00,
    "currency": "USD",
    "eventTimestamp": "2026-05-15T14:02:11Z"
  }'
```

List recent events for an account:

```bash
curl "http://localhost:8080/events?account=acct-123"
```

Query a balance (proxied through the Gateway):

```bash
curl "http://localhost:8080/accounts/acct-123/balance"
```

## Running tests

```bash
make test
```

Test coverage:

- **Core** — idempotency (`AccountServiceTest`, `EventServiceTest`), out-of-order balance (`AccountServiceTest`), validation incl. unknown type (`EventControllerTest`).
- **Resiliency** — real Resilience4j breaker transitions to `OPEN` after repeated Account Service failures (`CircuitBreakerIntegrationTest`); `503` mapping on `POST /events` and balance (`EventControllerTest`, `BalanceControllerTest`).
- **Trace propagation** — incoming `X-Trace-Id` is honored and generated when absent (`TraceIdFilterTest`, `EventControllerTest`).
- **Integration** — full Gateway → Account flow with WireMock (`GatewayAccountIntegrationTest`).

## Makefile targets

| Target | Description |
|---|---|
| `make clean` | Remove build artifacts |
| `make compile` | Compile both services |
| `make test` | Run all tests |
| `make build` | Package JARs |
| `make docker-build` | Build Docker images |
| `make docker-up` | Start via Docker Compose |
| `make docker-down` | Stop containers |

## Configuration

| Variable | Service | Default | Description |
|---|---|---|---|
| `ACCOUNT_SERVICE_URL` | Gateway | `http://localhost:8081` | Account Service base URL |
| `ACCOUNT_SERVICE_CONNECT_TIMEOUT` | Gateway | `2s` | Max time to establish a connection to the Account Service |
| `ACCOUNT_SERVICE_READ_TIMEOUT` | Gateway | `3s` | Max time to wait for an Account Service response |
| `RECENT_EVENT_LIMIT` | Gateway | `10` | Max events returned per account |
| `RECENT_TRANSACTION_LIMIT` | Account | `10` | Max transactions in account details |
| `SERVER_PORT` | Both | `8080` / `8081` | HTTP port |

## Observability

- **Structured JSON logs** with `traceId`, `service`, `timestamp`, and `level`
- **Trace propagation** via `X-Trace-Id` header (generated at Gateway, forwarded to Account Service)
- **Health checks** at `GET /health` on both services (includes DB connectivity)
- **Custom metrics** at `GET /metrics/custom` on both services
