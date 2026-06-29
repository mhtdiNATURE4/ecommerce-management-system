# Backend Trim Plan — Align Backend to Frontend + Project Requirements

## Context for Copilot

This is a Spring Boot + React e-commerce MIS course project. The required scope is:
1. Product display + shopping/cart/checkout flow.
2. A market-basket recommendation engine: "customers who buy A also buy B."
3. Customer segmentation based on purchase value/quality.
4. Three report types: periodical (e.g. weekly dashboard), threshold alerts (low stock), and on-demand/ad-hoc reports run by the manager.
5. A reasonably clean, scalable Web API / DB design — but NOT production-grade distributed-systems infrastructure.

The backend was built far beyond this scope (event-driven outbox pattern, distributed cron locking, a standalone image-proxy microservice, Prometheus metrics, async notification executors, concurrency stress tests, etc.). The frontend never calls most of this. The task is to **delete the unused infrastructure** while preserving every endpoint and behavior the frontend actually depends on.

**Ground rule: do not delete or change anything in this list of confirmed frontend-used endpoints without first re-checking the frontend.** Treat the "KEEP" list below as the source of truth for what must keep working after every step.

---

## Endpoints confirmed used by the frontend (KEEP — do not break)

| Method | Path | Used in |
|---|---|---|
| POST | `/api/auth/register` | RegisterPage.jsx |
| POST | `/api/auth/login` | LoginPage.jsx |
| GET | `/api/products` | ProductsPage.jsx |
| GET | `/api/products/{id}` | ProductDetailsPage.jsx, CartPage.jsx |
| GET | `/api/categories` | ProductsPage.jsx |
| GET | `/api/cart` | CartPage.jsx, CustomerLayout.jsx, CheckoutPage.jsx |
| POST | `/api/cart` | ProductDetailsPage.jsx, ProductsPage.jsx |
| PUT | `/api/cart/{cartItemId}/quantity` | CartPage.jsx |
| DELETE | `/api/cart/{cartItemId}` | CartPage.jsx |
| GET | `/api/addresses` | AddressesPage.jsx, CheckoutPage.jsx |
| POST | `/api/addresses` | AddressForm.jsx |
| POST | `/api/orders/checkout` | CheckoutPage.jsx |
| GET | `/api/orders` | OrdersPage.jsx |
| GET | `/api/orders/all` | AdminOrdersPage.jsx, AdminDashboardPage.jsx |
| PUT | `/api/orders/{id}/process` | AdminOrdersPage.jsx |
| PUT | `/api/orders/{id}/complete` | AdminOrdersPage.jsx |
| PUT | `/api/orders/{id}/cancel` | AdminOrdersPage.jsx |
| GET | `/api/recommendations` | RecommendationAnalyticsPage.jsx |
| GET | `/api/recommendations/product/{id}` | ProductDetailsPage.jsx |
| GET | `/api/admin/segments` | CustomerSegmentationPage.jsx |
| GET | `/api/admin/dashboard` | AdminDashboardPage.jsx |
| GET | `/api/admin/basket` | (admin basket view — confirm page if present; market basket analysis) |
| GET | `/api/admin/reports` | AdminReportsPage.jsx |
| GET | `/api/admin/reports/low-stock?threshold=10` | AdminReportsPage.jsx, AdminDashboardPage.jsx |
| POST | `/api/admin/reports/{id}/run` | AdminReportsPage.jsx |
| GET | `/api/admin/reports/{id}/history` | AdminReportsPage.jsx |
| GET | `/api/admin/reports/{id}/download` | AdminReportsPage.jsx (raw fetch, not via api.js) |
| POST | `/api/payments` | checkout flow (PaymentController) |

Everything in this table must keep returning the same shape it does today. If a step below would change a DTO field these pages read, stop and flag it instead of proceeding.

---

## Confirmed NOT used by frontend (safe to remove)

- `GET /api/admin/users` (`AdminUserController`)
- `GET /api/user/all`, `GET /api/user/all/paged` (`UserController`)
- `GET /api/products/paged`
- `GET /api/categories/paged`
- `GET /api/orders/paged`, `GET /api/orders/all/paged`
- `GET /api/admin/reports/summary`
- `GET /api/admin/reports/schedules`, `POST /api/admin/reports/schedules`, `POST /api/admin/reports/schedules/{id}/pause`, `POST /api/admin/reports/schedules/{id}/resume`, `DELETE /api/admin/reports/schedules/{id}`
- `GET /api/images/placeholder` (`ImageController`) — frontend renders images via plain `<img src>` / picsum.photos URLs, never calls this
- The entire `image-proxy/` microservice (separate Maven module, own Dockerfile) — zero references anywhere in frontend

---

## Step-by-step removal plan

Work through these in order. Each step is independently buildable/testable — after each one, run `mvn test` (or `./mvnw test`) and confirm the app still starts before moving to the next.

### Step 1 — Delete the standalone `image-proxy` module entirely
- Delete the whole `image-proxy/` directory at the project root.
- Remove any reference to it from root-level `docker-compose.yml` (if it defines an `image-proxy` service) and from `.github/workflows/*` CI files if they build/test it.
- Delete `backend/src/main/java/com/market/ecommerce/controller/ImageController.java` (and its test, if any) — it's dead code calling a feature the frontend never uses.

### Step 2 — Remove the reactive/event infrastructure (outbox pattern)
Delete:
- `backend/src/main/java/com/market/ecommerce/outbox/` (entire package: `OutboxEventProcessor`, `OutboxMetrics`, and their tests)
- `backend/src/main/java/com/market/ecommerce/entity/OutboxEvent.java`
- `backend/src/main/java/com/market/ecommerce/repository/OutboxEventRepository.java`
- `backend/src/main/java/com/market/ecommerce/config/OutboxProperties.java`
- `backend/src/main/java/com/market/ecommerce/event/` (entire package: `OrderCreatedEvent`, `OrderCreatedPublisher`, `OrderEventListener`, `PaymentInitiatedEvent`, `PaymentInitiatedPublisher`, `PaymentEventListener`, and the disabled-IT test)
- Flyway migrations `V5__create_outbox_event.sql`, `V6__add_next_retry_at.sql`, `V7__fix_null_versions.sql` — **do not delete these files** if the app is already deployed anywhere with a real database, since Flyway migrations are append-only history. Instead, leave the migrations in place (harmless extra table) but remove all Java code that reads/writes to them. If this is purely a fresh local/course-submission database, it's fine to also drop these migration files and re-baseline — your call given how the DB is currently set up.
- Wherever `OrderService` or `PaymentService` currently publish events instead of calling things directly (e.g. `paymentInitiatedPublisher.publish(...)`), inline the logic instead: have the service call `notificationService` (simplified, see Step 4) or just perform the side effect directly and synchronously. Search for every usage of `OrderCreatedPublisher` and `PaymentInitiatedPublisher` before deleting the event classes, so nothing is left calling a deleted class.

### Step 3 — Remove ShedLock and the report scheduler
Delete:
- `backend/src/main/java/com/market/ecommerce/reports/scheduler/ReportScheduler.java`
- `backend/src/main/java/com/market/ecommerce/reports/config/ShedLockConfig.java`
- `backend/src/main/java/com/market/ecommerce/reports/entity/ReportSchedule.java`
- `backend/src/main/java/com/market/ecommerce/reports/repository/ReportScheduleRepository.java`
- `backend/src/main/java/com/market/ecommerce/reports/dto/CreateScheduleRequest.java`, `ReportScheduleResponse.java`
- `backend/src/test/java/com/market/ecommerce/reports/ReportSchedulerCronTest.java`
- `backend/src/test/java/com/market/ecommerce/reports/ShedLockMigrationIT.java`
- Flyway migration `V8__create_shedlock_table.sql` (same caveat as Step 2 about live databases)
- In `ReportsController` and `ReportsControllerOpenApi`, remove the 5 schedule-related endpoint methods: `getSchedules`, `createSchedule`, `pauseSchedule`, `resumeSchedule`, `deleteSchedule`. Also remove `getSummary` (the `/summary` endpoint) since it's unused.
- In `ReportService` / `ReportServiceImpl`, remove the corresponding schedule-management methods, keeping only: list report definitions, get low-stock products, run a report now, get execution history, download latest execution.
- In `pom.xml`, remove the `shedlock-spring` and `shedlock-provider-jdbc-template` dependencies.

### Step 4 — Simplify `NotificationService`
Replace the current 200-line version (custom `ThreadPoolExecutor`, `MeterRegistry`, dead-letter alerting, `@PreDestroy` shutdown hook) with a minimal synchronous version that just sends the order confirmation email directly via `JavaMailSender`, with a simple try/catch and log line on failure. Delete the `notifyDeadLetter` method entirely (it only existed to support the now-deleted outbox dead-letter flow). Remove the `MeterRegistry` dependency from this class.

### Step 5 — Remove Micrometer/Actuator/Prometheus
- In `pom.xml`, remove `spring-boot-starter-actuator` and `micrometer-registry-prometheus`.
- Search the codebase for any remaining `MeterRegistry` injections (there may be one or two left in payment/order services) and remove them along with whatever metric-recording calls depended on them — these were only used for production observability dashboards, not for anything the frontend reads.
- Remove any actuator-related config from `application.properties`/`application.yml` (e.g. `management.endpoints.*`).

### Step 6 — Remove the concurrency stress-test suite
Delete the entire `backend/src/test/java/com/market/ecommerce/concurrency/` package: `CancelConcurrencyTest`, `CheckoutConcurrencyTest`, `EndToEndConcurrentCheckoutIT`, `NegativeStockGuardTest`, `RepositoryAtomicDecrementStressTest`. Also delete the root-level `ConcurrentCheckoutIT.java`. These test for race conditions under concurrent load — appropriate for a thesis-level system, not needed for grading a course MIS project. Keep `OrderServiceTest.java` and the basic ordering tests (`OrderLifecycleServiceTest`, `OrderPagingOrderingTest`) since those test normal business logic, not concurrency edge cases — though see Step 7 about the paging tests specifically.

### Step 7 — Remove unused pagination
- In `ProductController`, delete the `GET /paged` endpoint and its corresponding `ProductService` method (keep the regular unpaged `GET /` and `GET /{id}`).
- In `CategoryController`, delete the `GET /paged` endpoint and corresponding service method.
- In `OrderController`, delete `GET /paged` and `GET /all/paged` and their corresponding service methods.
- Delete `OrderPagingOrderingTest.java` since it tests the paging behavior you're removing.
- Double check no other controller method internally relies on the paged repository methods before deleting them from the repositories.

### Step 8 — Remove unused user-listing endpoints
- Delete `AdminUserController.java` entirely (only exposes `GET /api/admin/users`, unused).
- In `UserController`, delete the `/all` and `/all/paged` endpoints, keeping only `/profile`. Remove the now-unused service methods backing them in `UserService`.

### Step 9 — Simplify the Idempotency-Key requirement on payments (optional, discuss with your team first)
`PaymentController.makePayment` currently *requires* an `Idempotency-Key` header and returns 400 if missing — this is real-payment-gateway-grade safety with no corresponding UI (the frontend's checkout flow doesn't send this header explicitly, it likely works because of an interceptor or default — verify before changing). If you confirm the frontend doesn't send this header at all and payments still work, that suggests the requirement is dead weight; if removing it doesn't break checkout, simplify the method to not require it. **Do this one carefully and test checkout end-to-end afterward**, since payments touch order state.

### Step 10 — Clean up `pom.xml` further
After Steps 1–6, also reconsider:
- `opencsv` — keep if a report export still uses it (the `/download` endpoint likely produces CSV — verify before removing).
- `commons-math3` — check whether `RecommendationService`, `MarketBasketService`, or `CustomerSegmentationService` actually import anything from it. If not used, remove; these services were implemented with plain Java collections in the version inspected.
- `spring-dotenv` — fine to keep, harmless and useful for local dev.

### Step 11 — Final verification pass
1. `mvn clean test` — should pass with the now-much-smaller test suite.
2. Start the backend and run through every row in the "KEEP" table above manually (or via Postman/curl) to confirm responses are unchanged.
3. Run the frontend (`npm run dev`) against the trimmed backend and click through: register → login → browse products → view product detail (check recommendations show) → add to cart → checkout → view orders → admin login → dashboard → admin orders → admin reports (run a report, view history, download) → customer segmentation page → recommendation analytics page.
4. Re-run `grep -rn "OutboxEvent\|ShedLock\|MeterRegistry\|ReportSchedule\|image-proxy" backend/src` to confirm no dangling references were left behind from partial deletions.

---

## What to explicitly NOT touch

- `RecommendationService` / `RecommendationController` — core requirement, well-scoped, keep as-is.
- `MarketBasketService` / `MarketBasketController` — core requirement, well-scoped, keep as-is.
- `CustomerSegmentationService` / `CustomerSegmentationController` — core requirement, well-scoped, keep as-is.
- `DashboardService` / `DashboardController` — feeds your periodical report, keep as-is.
- `ReportsController`'s non-scheduling endpoints (list, run, history, download, low-stock) and `ReportDefinition`/`ReportExecution` entities — this is your three-report-types requirement, keep as-is.
- JWT auth, Cart, Orders, Products, Categories, Addresses, Security config — standard CRUD the frontend depends on directly.
- Flyway core setup and `V1`–`V4`, `V9` migrations.

---

## Suggested first prompt to paste into Copilot Chat in VS Code

> I'm trimming this Spring Boot backend down to match my course project requirements and my existing React frontend — removing infrastructure that was over-engineered beyond scope (event-driven outbox pattern, ShedLock distributed scheduling, a separate image-proxy microservice, Prometheus metrics, concurrency stress tests, unused pagination and user-listing endpoints). I have a full step-by-step plan in `BACKEND_TRIM_PLAN.md` in the repo root — follow it exactly, one step at a time, running `mvn test` after each step before moving to the next. Start with Step 1 and tell me what you're about to delete before doing it.
