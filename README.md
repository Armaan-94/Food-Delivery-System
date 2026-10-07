# Food Delivery Platform

A backend food delivery platform built as Spring Cloud microservices. Clients talk to a single API gateway, sign in with a JWT, and manage users, orders, delivery partners and deliveries through REST endpoints. There is no frontend; use curl, Postman or any HTTP client.

**Stack:** Java 17 · Spring Boot 3.5 · Spring Cloud 2025.0 (Gateway, Eureka, Config) · Spring Security (JWT) · MySQL 8

## What it does

| Area | Capabilities |
|---|---|
| Accounts | Sign up, log in, view/update/delete your own account; administrators manage everyone |
| Orders | List orders with paging and sorting, fetch one order (users see only their own, administrators see all) |
| Delivery partners | Create, read, update, delete (administrators) |
| Deliveries | Create for an existing order, assign a partner, move through a validated status lifecycle (administrators) |

Not included yet: placing an order (orders are read-only; see `db/samples/sample-orders.sql`), restaurants/menus, and payments.

## Architecture

```
                     ┌───────────────┐   routes via Eureka (lb://)
 client ──JWT──────► │  api-gateway  │ ───────────────┬──────────────┬──────────────┐
                     └──────┬────────┘                ▼              ▼              ▼
                            │ /auth/signup,    ┌─────────────┐ ┌──────────────┐ ┌───────────────┐
                            │ /auth/login      │ user-service│ │order-service │ │delivery-service│
                            ▼                  └──────▲──────┘ └──────▲───────┘ └───────┬───────┘
                     ┌──────────────┐                │               └─ order check ───┘
                     │ auth-service │ ── internal ───┘                  (caller's JWT forwarded)
                     └──────────────┘    (service token)
 config-server ── configuration for every service        service-discovery ── Eureka registry
```

| Service | Port | Database | Role |
|---|---|---|---|
| `config-server` | 8888 | – | Serves the property files in `config-server/src/main/resources/config-repo` |
| `service-discovery` | 8761 | – | Eureka registry (HTTP Basic protected) |
| `api-gateway` | 9004 | – | Single entry point; verifies the JWT, routes requests |
| `auth-service` | 9005 | – (stateless) | Sign-up and login; issues JWTs; locks out repeated failed logins |
| `user-service` | 7575 | `user_db` | Owns accounts: profile, role, BCrypt password hash |
| `order-service` | 8085 | `order_db` | Read access to orders |
| `delivery-service` | 8081 | `delivery_db` | Delivery partners and deliveries |
| `common` | – | – | Shared library: JWT security, error handling, response envelope |

Each business service owns its own database; no service reads another's tables.

## Security model

1. `POST /auth/login` returns a signed JWT (HS256) carrying the user's id, email and role (`USER` or `ADMIN`).
2. The gateway verifies the token locally (no per-request call to auth-service or the database) and forwards it unchanged.
3. **Every service verifies the token again** and enforces roles itself with `@PreAuthorize`. Calling a service directly, bypassing the gateway, gains nothing without a valid token.
4. auth-service talks to user-service on `/internal/**` with a short-lived `SERVICE` token. The gateway has no route to `/internal`.
5. delivery-service forwards the caller's own token when it checks an order with order-service, so the caller's permissions apply.

| Endpoint group | Who may call it |
|---|---|
| `/auth/signup`, `/auth/login` | Anyone |
| `/api/users/me`, `/api/users/{id}` (GET/PUT/DELETE) | The account owner or an administrator; only administrators can change roles |
| `/api/users` (GET, POST) | Administrators |
| `/api/orders`, `/api/orders/{id}` | Any signed-in user, limited to their own orders; administrators see all |
| `/api/partners/**`, `/api/deliveries/**` | Administrators |

Passwords are stored as BCrypt hashes and never returned. Secrets are never committed: configuration files contain `${PLACEHOLDERS}` that are filled from environment variables. Tokens last 60 minutes and there are no refresh tokens, so a deleted account's token stays valid until it expires.

## API

All requests except sign-up/login need `Authorization: Bearer <token>`. Every response uses one envelope:

```json
{ "success": true, "message": "Delivery created.", "data": { } }
{ "success": false, "message": "Order not found with id: 42" }
```

Errors never contain stack traces or database details.

| Method & path | Description |
|---|---|
| `POST /auth/signup` | `{name, email, password}` (password 8–72 chars) → creates a `USER` account |
| `POST /auth/login` | `{email, password}` → `{accessToken, tokenType, expiresIn, user}`; `429` after 5 consecutive failures (15 min lockout) |
| `GET /api/users` · `POST /api/users` | List / create users (admin) |
| `GET /api/users/me` | The signed-in user |
| `GET · PUT · DELETE /api/users/{id}` | Read / update / delete a user |
| `GET /api/orders?page&size&sortBy&direction` | Paged orders; `size` ≤ 100; `sortBy` ∈ `id, customerName, totalAmount, orderStatus, orderTimestamp` |
| `GET /api/orders/{id}` | One order |
| `GET · POST /api/partners`, `GET · PUT · DELETE /api/partners/{id}` | Partners: `{name, phoneNumber?, vehicleType, available?}` |
| `POST /api/deliveries` | `{orderId, partnerId?, pickupLocation, dropoffLocation}`; the order must exist; `PENDING`, or `ASSIGNED` when a partner is given |
| `GET /api/deliveries` · `GET · DELETE /api/deliveries/{id}` | List / read / delete |
| `PATCH /api/deliveries/{id}/status` | `{status, partnerId?}` |

Delivery lifecycle: `PENDING → ASSIGNED → PICKED_UP → OUT_FOR_DELIVERY → DELIVERED`, and `PENDING` or `ASSIGNED → CANCELLED`. A partner is required from `ASSIGNED` onwards.

## Getting started

### Option A: Docker Compose

```bash
cp .env.example .env      # then fill in every value
docker compose up --build
```

The gateway is on `http://localhost:9004`. The database schema in `db/init` is applied automatically on first start.

> The Compose setup has not been run in CI. If something fails, the manual steps below are the reference.

### Option B: run it by hand

Requires JDK 17 and MySQL 8. The included `mvnw` wrapper means Maven itself is optional.

1. **Create the databases.** Create an application user, then load the schema and grants:
   ```sql
   CREATE USER 'food_delivery'@'%' IDENTIFIED BY '<password>';
   ```
   ```bash
   mysql -u root -p < db/init/01-schema.sql
   mysql -u root -p < db/init/02-grants.sql
   ```
2. **Set the environment variables** from `.env.example` in every terminal you start a service from (`MYSQL_HOST` and `MYSQL_PORT` are optional and default to `localhost` and `3306`). `JWT_SECRET` must be at least 32 characters (`openssl rand -base64 48`).
3. **Build:** `./mvnw clean package -DskipTests`
4. **Start in this order** (`./mvnw -pl <module> spring-boot:run`, or `java -jar <module>/target/<module>-0.0.1-SNAPSHOT.jar`):
   `config-server` → `service-discovery` → `user-service`, `order-service`, `delivery-service` → `auth-service` → `api-gateway`

   `user-service` creates the first administrator from `ADMIN_EMAIL` / `ADMIN_PASSWORD` if it does not exist.

### Try it

```bash
# sign up and log in
curl -X POST localhost:9004/auth/signup -H 'Content-Type: application/json' \
     -d '{"name":"Asha","email":"asha@example.com","password":"a-long-password"}'
TOKEN=$(curl -s -X POST localhost:9004/auth/login -H 'Content-Type: application/json' \
     -d '{"email":"asha@example.com","password":"a-long-password"}' | jq -r .data.accessToken)

curl localhost:9004/api/users/me -H "Authorization: Bearer $TOKEN"
curl "localhost:9004/api/orders?size=5&sortBy=totalAmount&direction=asc" -H "Authorization: Bearer $TOKEN"
```

## Tests

```bash
./mvnw test
```

The tests need no MySQL, config server or Eureka. Each service is tested through its real HTTP layer and security filters with real signed JWTs, against an in-memory H2 database. Run `./mvnw test -pl user-service -am` for one service.

## Repository layout and conventions

```
pom.xml               parent/aggregator POM: Java and Spring versions in one place
common/               shared library (JWT security, exceptions, ApiResponse)
config-server/        config-repo/ holds the served property files
service-discovery/  api-gateway/  auth-service/  user-service/  order-service/  delivery-service/
db/init/              schema and grants; db/samples/ is optional demo data
Dockerfile · docker-compose.yml · .env.example
```

- **Names:** modules and Eureka service names are `kebab-case` (`user-service`); Java packages are lowercase (`com.fooddelivery.userservice`); the main class is `<Service>Application`. All modules share `groupId com.fooddelivery` and version `0.0.1-SNAPSHOT`.
- **Layers:** `controller → service → repository`, plus `dto`, `model`, `config`, and `client` for outgoing calls. Controllers contain no try/catch: a single `GlobalExceptionHandler` in `common` maps exceptions to HTTP responses.
- **Types:** request/response classes end in `Dto` and are Java records; JSON properties are `camelCase`; database columns are `snake_case`; ids are `Long`.
- **Terminology:** a delivery is carried out by a **partner** (not "delivery person" or "user").

## Known limitations

- Login lockout is in memory, per instance, and resets on restart; it can also be used to lock a known email out temporarily.
- One shared HS256 secret signs and verifies tokens; rotating it logs everyone out. Asymmetric keys would let only auth-service sign.
- Traffic between services is plain HTTP; run behind TLS or a private network in production.
- The database user is shared by the three services (each limited to its own schema in practice, not by grants).
- No rate limiting at the gateway, circuit breakers, or distributed tracing.
