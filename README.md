# Food Delivery Platform

A backend food delivery platform built as seven Spring Cloud microservices and one shared library. Clients talk to a single API gateway, sign in to get a JWT, and use REST endpoints to manage users, read orders, and run deliveries with delivery partners. There is no frontend: you use curl or Postman.

**Stack:** Java 17 · Spring Boot 3.5 · Spring Cloud 2025.0 (Gateway, Eureka, Config) · Spring Security with JWT · MySQL 8 · Maven

This document is a tutorial. Read it top to bottom once and you will understand how the whole system works, why it is built this way, and how to run and explain it.

**Contents**
1. [The big picture](#1-the-big-picture)
2. [Following one request through the system](#2-following-one-request-through-the-system)
3. [Security in depth](#3-security-in-depth)
4. [The services, one by one](#4-the-services-one-by-one)
5. [Data model](#5-data-model)
6. [Configuration and startup](#6-configuration-and-startup)
7. [Run it](#7-run-it)
8. [Guided demo](#8-guided-demo)
9. [API reference](#9-api-reference)
10. [Tests](#10-tests)
11. [Code map and reading order](#11-code-map-and-reading-order)
12. [Design decisions and trade-offs](#12-design-decisions-and-trade-offs)
13. [Known limitations](#13-known-limitations)

---

## 1. The big picture

```
                      ┌──────────────┐        lb://user-service
  client ──Bearer──►  │ api-gateway  │ ─────────────────────────────►  user-service     ─► user_db
          JWT         │  :9004       │ ──── lb://order-service ──────►  order-service    ─► order_db
                      │              │ ──── lb://delivery-service ───►  delivery-service ─► delivery_db
                      └──────┬───────┘                                       │
                             │ /auth/signup, /auth/login                     │ checks the order exists
                             ▼                                               ▼ (forwards the caller's JWT)
                      ┌──────────────┐   /internal/users/*            order-service
                      │ auth-service │ ──── service token ─────────►  user-service
                      │  :9005       │
                      └──────────────┘

   config-server :8888  hands every service its configuration at startup
   service-discovery :8761  (Eureka) lets services find each other by name
```

| Module | Port | Database | What it does |
|---|---|---|---|
| `config-server` | 8888 | none | Serves the property files in `config-server/src/main/resources/config-repo` |
| `service-discovery` | 8761 | none | Eureka registry, protected with HTTP Basic |
| `api-gateway` | 9004 | none | The only public entry point. Verifies the JWT and routes the request |
| `auth-service` | 9005 | none (stateless) | Sign-up and login. Issues JWTs. Locks out repeated failed logins |
| `user-service` | 7575 | `user_db` | Owns accounts: profile, role, BCrypt password hash |
| `order-service` | 8085 | `order_db` | Read access to orders |
| `delivery-service` | 8081 | `delivery_db` | Delivery partners and deliveries |
| `common` | none | none | Shared library for the servlet services: JWT security, exceptions, response envelope |

Three ideas explain most of the design:

- **Every service checks the token itself.** The gateway checks it too, but only as a first line of defence.
- **Every service owns its data.** No service reads another service's tables; they call each other's APIs.
- **One response and error format everywhere**, so clients and tests handle every service the same way.

What the app can do: sign up and log in; view, update and delete accounts; list and fetch orders (users see only their own); manage delivery partners; create deliveries for orders and move them through a status lifecycle. What it cannot do yet: place an order (orders are read-only), restaurants and menus, payments.

---

## 2. Following one request through the system

The best way to learn the architecture is to follow two requests end to end.

### A. Login: `POST /auth/login`

1. **Gateway** (`api-gateway`). The path is one of the two public routes, so no token is needed. `GatewayRoutes` forwards it to `lb://auth-service`; Eureka resolves that name to a real host and port.
2. **`AuthController`** validates the body (`@Valid`) and calls `AuthService.login`.
3. **`LoginAttemptService.assertNotLocked`** rejects the request with `429` if this email has failed too many times recently.
4. **`UserServiceClient.verify`** calls `user-service` at `/internal/users/verify`. It signs a short-lived `SERVICE` token (2 minutes) with `JwtTokenService` and sends it as the Bearer token.
5. **`user-service`** (`InternalUserController` → `UserService.verifyCredentials`) looks up the email and compares the password with BCrypt. If the email does not exist it still runs a BCrypt comparison against a dummy hash, so response time does not reveal which emails exist. Any failure gives the same `401` message.
6. Back in `auth-service`: on failure it records the failed attempt; on success it clears the counter and calls `JwtTokenService.issueUserToken`.
7. The response is `{success, data: {accessToken, tokenType: "Bearer", expiresIn: 3600, user}}`.

The token contains `sub` (user id), `email`, `name`, `role`, `iss`, `iat`, `exp` and a unique `jti`.

### B. Creating a delivery: `POST /api/deliveries`

1. **Gateway.** `SecurityConfig` decodes the token (signature, issuer, expiry), then requires the role `USER` or `ADMIN` for `/api/**`. A `SERVICE` token or no token never gets further. The `Authorization` header is forwarded unchanged to `lb://delivery-service`.
2. **`delivery-service`.** The auto-configured security chain from `common` (`JwtSecurityAutoConfiguration`) validates the token again and builds the authentication. `DeliveryController` is annotated `@PreAuthorize("hasRole('ADMIN')")`, so a `USER` token is rejected with `403` here.
3. **`DeliveryService.createDelivery`:**
   - `OrderClient.assertOrderExists` calls `order-service` (`GET /api/orders/{id}`). A `BearerTokenForwardingInterceptor` copies the caller's own token onto that outgoing request, so `order-service` applies the caller's permissions. A missing order becomes `404`; an unreachable `order-service` becomes `503`.
   - If a `partnerId` was given, the partner must exist (`404` otherwise) and be available (`400` otherwise).
   - `DeliveryRepository.insert` saves it. Status is `ASSIGNED` when a partner was given, else `PENDING`. A second delivery for the same order violates a unique key and becomes `409`.
4. The controller returns `201` with `{success, message, data}`.

If anything throws, the `GlobalExceptionHandler` in `common` turns it into the standard error envelope with the right status code. Controllers contain no try/catch.

---

## 3. Security in depth

### The token

HS256 JWT signed with a shared secret (`JWT_SECRET`, at least 32 characters; the service refuses to start without it). Issued only by `auth-service`. Lasts 60 minutes. Three roles exist:

| Role | Who | What it can do |
|---|---|---|
| `USER` | Anyone who signs up | Own account, own orders |
| `ADMIN` | Created by the bootstrap runner or by another admin | Everything, including partners and deliveries |
| `SERVICE` | `auth-service` only, 2-minute tokens | Only `/internal/**` on `user-service` |

### Defence in depth

| Layer | What it enforces |
|---|---|
| Gateway | Valid signature, issuer and expiry; role `USER` or `ADMIN` on `/api/**`; only the routes in `GatewayRoutes` exist, so `/internal/**` is unreachable from outside |
| Each service | The same token validation again, plus `@PreAuthorize` on controllers; ownership rules in the service layer |
| Data | BCrypt hashes only; password and hash are never in any response DTO |

Because every service validates the token, calling `user-service` directly on port 7575 without a valid token gives `401`. The gateway is a convenience and a first filter, not the only gate.

### Access rules

| Endpoints | Who may call them |
|---|---|
| `/auth/signup`, `/auth/login` | Anyone |
| `/api/users/me`, `/api/users/{id}` (GET, PUT, DELETE) | The account owner or an admin. Only an admin can change a role |
| `/api/users` (GET, POST) | Admins |
| `/api/orders`, `/api/orders/{id}` | Any signed-in user, limited to their own orders. Admins see all. Someone else's order looks exactly like a missing one (`404`) |
| `/api/partners/**`, `/api/deliveries/**` | Admins |
| `/internal/users/**` | `SERVICE` token only |

### Other protections

- **Login lockout:** 5 consecutive failures lock that email for 15 minutes (`429`), even for the right password. In memory, per instance.
- **No information leaks:** a wrong password and an unknown email return the identical error; 500-level errors return a generic message while the details are logged; stack traces and SQL never reach the client.
- **Input validation:** Jakarta Bean Validation on every request body; page size capped at 100; sort fields checked against a whitelist (so `sortBy` can never reach SQL); SQL uses bound parameters.
- **No secrets in the repository:** configuration files hold `${PLACEHOLDERS}`; a test fails if a password or secret is hard-coded.

---

## 4. The services, one by one

### `common` (shared library)

Used by `auth-service`, `user-service`, `order-service` and `delivery-service`. It is **not** used by the gateway, which is reactive (WebFlux) and cannot have Spring MVC on its classpath.

| Piece | Purpose |
|---|---|
| `JwtSecurityAutoConfiguration` | Spring Boot auto-configuration: builds the JWT decoder/encoder, the security filter chain (stateless, every request authenticated, JSON error bodies) and turns on `@PreAuthorize`. A service gets all of this just by depending on `common`. A service that defines its own `SecurityFilterChain` (auth-service) keeps its own |
| `JwtTokenService` | Issues user tokens and service tokens |
| `AuthenticatedUser` | The caller (id, email, role) read from the token; used for ownership checks |
| `BearerTokenForwardingInterceptor` | Copies the caller's token onto outgoing REST calls |
| `ApiException` + subclasses | `ValidationException` (400), `ResourceNotFoundException` (404), `DuplicateResourceException` (409), `AuthenticationFailedException` (401), `TooManyAttemptsException` (429), `ServiceUnavailableException` (503), `DatabaseAccessException` (500) |
| `GlobalExceptionHandler` | One `@RestControllerAdvice` that maps every exception to an HTTP status and the envelope |
| `ApiResponse<T>` | The `{success, message, data}` envelope |

### `config-server` and `service-discovery`

The config server serves property files from the classpath (`native` profile) and requires HTTP Basic. Eureka holds the registry and requires HTTP Basic too. Both expose an open `/actuator/health` for health checks.

### `api-gateway`

`SecurityConfig` verifies tokens and applies the role rule; `GatewayRoutes` declares exactly four routes (`auth-service`, `user-service`, `delivery-service`, `order-service`); `JsonSecurityErrorHandler` renders 401/403 in the standard envelope. The gateway adds timeouts (2 s connect, 15 s response).

### `auth-service`

Has no database. `AuthService` coordinates `LoginAttemptService` (lockout) and `UserServiceClient` (calls `user-service` over a load-balanced `RestClient` with a service token). It exists so that token issuing and brute-force protection live in one small place.

### `user-service`

The single owner of accounts. `UserController` is the public API; `InternalUserController` is for `auth-service`. `UserService` normalizes emails (trim, lowercase), hashes passwords with BCrypt, and enforces the "only admins change roles" rule. `AdminBootstrapRunner` creates the first admin from `ADMIN_EMAIL` and `ADMIN_PASSWORD` when the app starts, so a fresh database has a way to obtain an `ADMIN` token. Data access uses `JdbcTemplate` and `SimpleJdbcInsert`.

### `order-service`

Uses Spring Data JPA. `OrderService` decides what a caller can see (admin: all; user: `findByUserId`), validates paging and sorting, and returns DTOs inside a stable `PageDto` (Spring's own `Page` type is not meant to be serialized as JSON).

### `delivery-service`

Two aggregates: partners and deliveries, both with `JdbcTemplate` repositories. The status lifecycle lives in the `DeliveryStatus` enum:

```
PENDING ──► ASSIGNED ──► PICKED_UP ──► OUT_FOR_DELIVERY ──► DELIVERED
   │            │
   └────────────┴──► CANCELLED
```

A partner is required from `ASSIGNED` onwards, and the partner must be marked available when assigned. `DELIVERED` and `CANCELLED` are final.

---

## 5. Data model

Each service has its own database. The full DDL is in `db/init/01-schema.sql`.

```
user_db.users                         delivery_db.partners             order_db.food_orders
─────────────                         ────────────────────             ────────────────────
id            BIGINT PK               id            BIGINT PK          id              BIGINT PK
name          VARCHAR(100)            name          VARCHAR(100)       user_id         BIGINT   ← the account (JWT sub)
email         VARCHAR(255) UNIQUE     phone_number  VARCHAR(20) NULL   customer_name   VARCHAR(255)
password_hash VARCHAR(100)            vehicle_type  VARCHAR(50)        order_details   VARCHAR(1000) NULL
role          USER | ADMIN            available     BOOLEAN            total_amount    DECIMAL(10,2)
created_at    TIMESTAMP               created_at    TIMESTAMP          order_status    VARCHAR(30)
                                                                       order_timestamp DATETIME
                                      delivery_db.deliveries
                                      ──────────────────────
                                      id, order_id UNIQUE, partner_id NULL → partners(id) ON DELETE RESTRICT,
                                      status, pickup_location, dropoff_location, created_at, updated_at
```

Notes:
- `deliveries.order_id` and `food_orders.user_id` are plain numbers, not foreign keys, because they point into another service's database.
- A partner that has deliveries cannot be deleted (`RESTRICT`); the API answers `409`.
- Hibernate runs with `ddl-auto=validate` for `order-service`: it checks the entity against the table but never creates or alters it.

---

## 6. Configuration and startup

**How a service gets its configuration:**

1. Each service's own `application.properties` holds only its name and the config server address and credentials.
2. At startup it asks the config server for `application.properties` (shared: Eureka address, JWT settings, actuator settings) and `<service-name>.properties` (port, database URL, and so on).
3. Values like `${JWT_SECRET}` or `${MYSQL_PASSWORD}` are placeholders. They are filled from **environment variables of the service's own process**, so every service needs the variables it uses.

**Startup order** matters because of those dependencies:

```
MySQL → config-server → service-discovery → user-service, order-service, delivery-service → auth-service → api-gateway
```

After starting, give the services about 30 seconds to register with Eureka before routing through the gateway works.

**Environment variables**

| Variable | Used by | Meaning |
|---|---|---|
| `CONFIG_SERVER_USER`, `CONFIG_SERVER_PASSWORD` | all | Credentials for the config server |
| `EUREKA_USER`, `EUREKA_PASSWORD` | all | Credentials for Eureka |
| `JWT_SECRET` | all services | Token signing and verification key, at least 32 characters. It must be identical everywhere |
| `MYSQL_PASSWORD` | user, order, delivery | Password of the `food_delivery` database user |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | user-service | First administrator (password at least 8 characters) |
| `MYSQL_HOST`, `MYSQL_PORT`, `MYSQL_USER` | user, order, delivery | Optional; default `localhost`, `3306`, `food_delivery` |
| `CONFIG_SERVER_HOST`, `EUREKA_HOST` | all | Optional; default `localhost` |

---

## 7. Run it

### Option A: Docker Compose

```bash
cp .env.example .env      # fill in every value
docker compose up --build
```

The gateway is on `http://localhost:9004`. The schema in `db/init` is applied automatically on the first start. The compose file has not been run in an automated pipeline, so if it misbehaves, the manual steps below are the reference.

### Option B: by hand

Needs JDK 17 and MySQL 8. The `mvnw` wrapper downloads Maven for you.

**1. Create the databases.** Create the application user, then load the schema and grants:

```sql
CREATE USER 'food_delivery'@'%' IDENTIFIED BY '<choose a password>';
```
```bash
mysql -u root -p < db/init/01-schema.sql
mysql -u root -p < db/init/02-grants.sql
```

**2. Set environment variables** in every terminal you start a service from (generate a secret with `openssl rand -base64 48`):

```bash
# Git Bash / macOS / Linux
export CONFIG_SERVER_USER=config CONFIG_SERVER_PASSWORD=change-me
export EUREKA_USER=eureka        EUREKA_PASSWORD=change-me
export JWT_SECRET=<at least 32 random characters>
export MYSQL_PASSWORD=<the password from step 1>
export ADMIN_EMAIL=admin@example.com ADMIN_PASSWORD=change-me-too
```
```powershell
# PowerShell
$env:CONFIG_SERVER_USER="config"; $env:CONFIG_SERVER_PASSWORD="change-me"
$env:EUREKA_USER="eureka";        $env:EUREKA_PASSWORD="change-me"
$env:JWT_SECRET="<at least 32 random characters>"
$env:MYSQL_PASSWORD="<the password from step 1>"
$env:ADMIN_EMAIL="admin@example.com"; $env:ADMIN_PASSWORD="change-me-too"
```

**3. Build once:** `./mvnw clean package -DskipTests`

**4. Start each service in its own terminal, in this order:**

```bash
java -jar config-server/target/config-server-0.0.1-SNAPSHOT.jar
java -jar service-discovery/target/service-discovery-0.0.1-SNAPSHOT.jar
java -jar user-service/target/user-service-0.0.1-SNAPSHOT.jar
java -jar order-service/target/order-service-0.0.1-SNAPSHOT.jar
java -jar delivery-service/target/delivery-service-0.0.1-SNAPSHOT.jar
java -jar auth-service/target/auth-service-0.0.1-SNAPSHOT.jar
java -jar api-gateway/target/api-gateway-0.0.1-SNAPSHOT.jar
```

Check that each is healthy with `curl localhost:<port>/actuator/health`, and open `http://localhost:8761` (log in with the Eureka credentials) to see the registered services.

---

## 8. Guided demo

These steps walk through every important behaviour. Use Git Bash (or any shell with curl). Replace the credentials with the ones you set.

**1. Sign up, then log in**

```bash
curl -X POST localhost:9004/auth/signup -H 'Content-Type: application/json' \
  -d '{"name":"Asha","email":"asha@example.com","password":"a-long-password"}'

USER_TOKEN=$(curl -s -X POST localhost:9004/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"asha@example.com","password":"a-long-password"}' | jq -r .data.accessToken)

ADMIN_TOKEN=$(curl -s -X POST localhost:9004/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"admin@example.com","password":"change-me-too"}' | jq -r .data.accessToken)
```

Paste a token into jwt.io (or decode the middle part) to see the claims. Signing up again with the same email returns `409`.

**2. See the role rules**

```bash
curl localhost:9004/api/users/me -H "Authorization: Bearer $USER_TOKEN"     # 200: your own account
curl localhost:9004/api/users    -H "Authorization: Bearer $USER_TOKEN"     # 403: admins only
curl localhost:9004/api/users    -H "Authorization: Bearer $ADMIN_TOKEN"    # 200: everyone
curl localhost:9004/api/users                                               # 401: no token
curl -X POST localhost:9004/internal/users/verify -H "Authorization: Bearer $ADMIN_TOKEN"   # 403: not routable
curl localhost:7575/api/users                                               # 401: services check tokens themselves
```

**3. Create some orders.** There is no endpoint for placing orders, so insert two directly. Replace `2` with Asha's real id (shown by the signup response):

```sql
INSERT INTO order_db.food_orders (user_id, customer_name, order_details, total_amount, order_status)
VALUES (2, 'Asha', '2 x pizza', 24.50, 'PLACED'), (999, 'Someone Else', 'soup', 5.00, 'PLACED');
```

```bash
curl "localhost:9004/api/orders"                -H "Authorization: Bearer $USER_TOKEN"   # only Asha's order
curl "localhost:9004/api/orders"                -H "Authorization: Bearer $ADMIN_TOKEN"  # both
curl "localhost:9004/api/orders/2"              -H "Authorization: Bearer $USER_TOKEN"   # 404: not hers
curl "localhost:9004/api/orders?sortBy=x;drop"  -H "Authorization: Bearer $USER_TOKEN"   # 400: not a sortable field
curl "localhost:9004/api/orders?size=5&sortBy=totalAmount&direction=asc" -H "Authorization: Bearer $ADMIN_TOKEN"
```

**4. Run a delivery from start to finish (as admin)**

```bash
AUTH="Authorization: Bearer $ADMIN_TOKEN"; JSON='Content-Type: application/json'; G=localhost:9004

curl -X POST $G/api/partners -H "$AUTH" -H "$JSON" -d '{"name":"Ravi","phoneNumber":"9876543210","vehicleType":"Bike"}'
curl -X POST $G/api/deliveries -H "$AUTH" -H "$JSON" \
  -d '{"orderId":1,"pickupLocation":"12 Market Road","dropoffLocation":"48 Lake View"}'      # 201, PENDING
curl -X POST $G/api/deliveries -H "$AUTH" -H "$JSON" \
  -d '{"orderId":4242,"pickupLocation":"a","dropoffLocation":"b"}'                          # 404: that order does not exist

curl -X PATCH $G/api/deliveries/1/status -H "$AUTH" -H "$JSON" -d '{"status":"ASSIGNED"}'                  # 400: needs a partner
curl -X PATCH $G/api/deliveries/1/status -H "$AUTH" -H "$JSON" -d '{"status":"ASSIGNED","partnerId":1}'    # 200
curl -X PATCH $G/api/deliveries/1/status -H "$AUTH" -H "$JSON" -d '{"status":"DELIVERED"}'                 # 400: cannot skip steps
curl -X PATCH $G/api/deliveries/1/status -H "$AUTH" -H "$JSON" -d '{"status":"PICKED_UP"}'                 # 200
curl $G/api/deliveries/1 -H "$AUTH"
curl -X DELETE $G/api/partners/1 -H "$AUTH"                                                                # 409: partner has a delivery
```

**5. Trigger the login lockout.** Send six wrong passwords in a row. Attempts 1 to 5 return `401`; the sixth returns `429`, and so does the correct password until the 15 minutes pass:

```bash
for i in 1 2 3 4 5 6; do
  curl -s -o /dev/null -w "attempt $i: %{http_code}\n" -X POST localhost:9004/auth/login \
    -H 'Content-Type: application/json' -d '{"email":"asha@example.com","password":"wrong-password"}'
done
```

---

## 9. API reference

All endpoints except sign-up and login need `Authorization: Bearer <token>`. Every response uses one envelope, and errors never contain stack traces or database details:

```json
{ "success": true,  "message": "Delivery created.", "data": { } }
{ "success": false, "message": "Order not found with id: 42" }
```

| Method and path | Body or parameters | Result |
|---|---|---|
| `POST /auth/signup` | `{name, email, password}` (password 8 to 72 characters) | `201` the new `USER` |
| `POST /auth/login` | `{email, password}` | `200` `{accessToken, tokenType, expiresIn, user}`; `401` wrong credentials; `429` locked out |
| `GET /api/users` | none | Admin: all users |
| `POST /api/users` | `{name, email, password, role?}` | Admin: create a user |
| `GET /api/users/me` | none | The signed-in user |
| `GET`, `PUT`, `DELETE /api/users/{id}` | PUT: `{name, email, password?, role?}` | Owner or admin; only admins can change `role` |
| `GET /api/orders` | `page`, `size` (max 100), `sortBy`, `direction` | Paged orders. `sortBy`: `id`, `customerName`, `totalAmount`, `orderStatus`, `orderTimestamp` |
| `GET /api/orders/{id}` | none | One order |
| `GET`, `POST /api/partners` | `{name, phoneNumber?, vehicleType, available?}` | Admin |
| `GET`, `PUT`, `DELETE /api/partners/{id}` | as above | Admin; `409` if the partner has deliveries |
| `POST /api/deliveries` | `{orderId, partnerId?, pickupLocation, dropoffLocation}` | Admin; `404` unknown order or partner; `409` order already has a delivery |
| `GET /api/deliveries`, `GET`, `DELETE /api/deliveries/{id}` | none | Admin |
| `PATCH /api/deliveries/{id}/status` | `{status, partnerId?}` | Admin; `400` for an invalid transition or unknown status |

Status codes you will see: `400` bad input, `401` no or invalid token, `403` not allowed, `404` not found, `409` conflict, `429` too many login attempts, `503` a dependency is down.

---

## 10. Tests

```bash
./mvnw test                          # everything
./mvnw test -pl user-service -am     # one service (plus common)
```

106 tests, with no MySQL, Eureka or config server needed.

| Module | Tests | What they cover |
|---|---|---|
| `common` | 15 | Token signing and verification, tampered, expired, wrong-issuer and wrong-secret tokens; every exception mapped to the right status without leaking details |
| `config-server` | 6 | Serves shared and per-service files, only placeholders for secrets, rejects bad credentials, and scans the config files so no password can be hard-coded |
| `api-gateway` | 9 | Missing, invalid, expired, forged and service tokens are rejected before reaching a service; a valid token is forwarded unchanged; `/internal` is never reachable |
| `auth-service` | 24 | Lockout timing and reset, the user-service client's error translation, full sign-up and login flow |
| `user-service` | 19 | Role rules, ownership, no password in any response, BCrypt storage, duplicates, internal endpoints |
| `order-service` | 11 | Ownership, paging, sort whitelist, input limits |
| `delivery-service` | 22 | Whole lifecycle and invalid transitions, partner rules, null partner and stored locations, outage handling, order-service client |

How they work: each service is started as a real Spring application, requests go through the real HTTP and security layers with real signed JWTs, and the database is an in-memory H2 database in MySQL mode. Calls to other services are replaced with a mock or a mock HTTP server. The MySQL-specific schema (`db/init`) is not exercised by these tests, so a real-database check is a separate step.

---

## 11. Code map and reading order

```
pom.xml                         parent POM: Java and Spring versions, module list
common/                         shared library
config-server/                  config-repo/ holds the served property files
service-discovery/  api-gateway/  auth-service/  user-service/  order-service/  delivery-service/
db/init/                        schema and grants (db/samples/ is optional demo data)
Dockerfile · docker-compose.yml · .env.example · mvnw
```

Every service follows the same layout under `src/main/java/com/fooddelivery/<service>/`: `controller` → `service` → `repository`, plus `dto`, `model`, `config`, and `client` for outgoing calls.

**A good reading order:**

1. `common/.../security/JwtSecurityAutoConfiguration.java`: how every service is secured with almost no code of its own.
2. `common/.../web/GlobalExceptionHandler.java` and `common/.../exception/`: the single error-handling place.
3. `api-gateway/.../config/SecurityConfig.java` and `GatewayRoutes.java`: the edge.
4. `auth-service/.../service/AuthService.java`, `LoginAttemptService.java`, `client/UserServiceClient.java`: login.
5. `user-service/.../service/UserService.java` and `controller/InternalUserController.java`: the account owner.
6. `delivery-service/.../service/DeliveryService.java`, `model/DeliveryStatus.java`, `client/OrderClient.java`: the richest business logic.
7. `order-service/.../service/OrderService.java`: ownership and safe sorting.
8. `config-server/src/main/resources/config-repo/`: how configuration is supplied.
9. Any `*IntegrationTest.java`: the behaviour, written as examples.

**Naming conventions:** modules and Eureka names are `kebab-case`; Java packages are lowercase (`com.fooddelivery.userservice`); main classes are `<Service>Application`; request and response classes end in `Dto` and are Java records; JSON is `camelCase`, database columns `snake_case`, ids are `Long`; a delivery is carried out by a **partner**.

---

## 12. Design decisions and trade-offs

| Decision | Why | Cost |
|---|---|---|
| **Every service validates the JWT**, not only the gateway | Bypassing the gateway gains nothing; identity is cryptographically attested everywhere; no per-request call to an auth service | Each service needs the secret; key rotation touches all of them |
| **Authentication state lives in the token**, so auth-service is stateless | No session store, no database lookup per request; `auth-service` is cheap to scale | A deleted or demoted account keeps working until its token expires (60 min) |
| **`user-service` owns credentials; `auth-service` has no database** | One identity store, no sync problem, deleting a user truly removes login | Login needs `user-service` to be up |
| **Internal calls use a short-lived `SERVICE` token** and `/internal` has no gateway route | No static shared passwords; the endpoint cannot be reached from outside | One more token type to reason about |
| **Forward the caller's token to `order-service`** instead of a service token | The caller's permissions apply on the second hop; no privilege escalation | The caller must be allowed to read that order |
| **Database per service** | Services can change schemas independently; no hidden coupling through shared tables | No joins or cross-service transactions; consistency is by API call |
| **Remote order check is not inside a database transaction** | A slow `order-service` must not hold a database connection | The check and the insert are not atomic (a unique key guards duplicates) |
| **`common` is a Spring Boot auto-configuration** | Security and error handling are written once, and a service opts in by adding one dependency | It is servlet-only, so the reactive gateway has a small duplicate of the JWT settings |
| **One global exception handler and a typed exception hierarchy** | Consistent status codes, nothing leaks, controllers stay short | Every failure must be modelled as an `ApiException` or it becomes a generic 500 |
| **`JdbcTemplate` for users and delivery, JPA for orders** | Plain SQL is explicit and easy to explain; JPA shows paging and sorting with Spring Data | Two persistence styles in one project |
| **Names the columns in `SimpleJdbcInsert`** | Lets the database fill defaults such as `created_at`; omitting this caused a real failure on MySQL 8 | None |
| **State machine in an enum** | Rules are in one tested place, not scattered in `if` statements | Adding a status means updating `allowedNext` |
| **HS256 with a shared secret** | The simplest correct choice for a single deployment | See limitations: asymmetric keys are safer |

---

## 13. Known limitations

- **Tokens cannot be revoked.** They last 60 minutes and there are no refresh tokens or logout. A deleted or demoted account keeps its old access until expiry.
- **One shared signing secret.** Any service that holds it could mint tokens, and rotating it logs everyone out. Asymmetric keys (RS256) would let only `auth-service` sign.
- **Plain HTTP between services**, including the password sent from `auth-service` to `user-service` at login. Use TLS or a private network.
- **Login lockout is in memory.** It is per instance, resets on restart, and can be used to lock a known email out temporarily.
- **One database user** is shared by the three services (granted access to all three schemas).
- **No gateway rate limiting, circuit breakers or tracing.** Creating a delivery depends on `order-service` being up.
- **Admin safeguards are minimal.** An admin can demote or delete themselves and leave the system with no admin.
- **No order placement.** Orders are inserted by SQL; `food_orders` needs a `user_id` column, which an older database will not have.
- **Docker Compose is untested**, and the automated tests use H2 rather than MySQL.
