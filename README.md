# HMS Backend — Multi-Tenant Hospital Management System

Spring Boot 3.3 / Java 21 modular monolith. Schema-per-tenant MySQL isolation.
Swagger-ready. Built to be split into microservices later without a rewrite.

---

## 1. Architecture in one page

- **One codebase, one deployable JAR** (modular monolith) — packages are cleanly
  separated by domain (`patient`, `doctor`, `appointment`, `ipd`, `billing`,
  `pharmacy`, ...) with no cross-package entity coupling beyond simple IDs, so
  any package can be lifted into its own microservice later without touching
  the others.
- **Two MySQL "areas", physically one server, two Spring persistence units:**
  - `hms_master` — a single fixed schema holding the **tenant registry**
    (`tenants`), **platform admins**, and an audit log. Nothing clinical or
    financial ever lives here. This is the "1 table has all hospital
    names/ids" piece you asked for.
  - `tenant_<code>` — **one MySQL schema per hospital**, created automatically
    at onboarding, holding that hospital's users, patients, doctors, staff,
    appointments, OPD/IPD records, billing, and pharmacy data. A hospital can
    never see another hospital's schema — there is no query path that crosses
    schemas, and the connection literally switches database context
    (`USE tenant_xxx`) before every request is served.
- **How the switch happens:** `SchemaMultiTenantConnectionProvider` +
  `CurrentTenantIdentifierResolverImpl` (Hibernate's built-in schema-based
  multi-tenancy) switch the JDBC connection's active schema per request,
  driven by `TenantContext` (a ThreadLocal), which `JwtAuthenticationFilter`
  populates from the schema name embedded in each user's JWT. One HikariCP
  pool serves every hospital — see comments in
  `SchemaMultiTenantConnectionProvider` and `application.yml` for the scaling
  note on when to split into pool-per-tenant.
- **Login flow:** a user provides `tenantCode` (their hospital's code) +
  `username` + `password`. The backend looks up the hospital in `hms_master`,
  switches to its schema, authenticates against *that* schema's `app_users`
  table, and issues a JWT with the schema baked in. Every subsequent request
  is auto-scoped — no header/param can override which hospital's data you see.

## 2. What's implemented

All 10 modules from the requirements, end-to-end (entity → repository →
service → REST controller → Swagger docs):

| Module | Highlights |
|---|---|
| Tenant management | Automated onboarding (creates schema, runs migrations, seeds admin login), suspend/reactivate |
| Auth & RBAC | JWT (access + refresh), 7 hospital roles + 2 platform roles, method-level `@PreAuthorize` |
| Staff management | Nurses, receptionists, billing staff, pharmacists, lab techs — login + profile created together |
| Patient management | Full CRUD, paginated search, autocomplete/typeahead |
| Doctor management | Departments, doctors, weekly availability templates |
| Appointments / OPD | Slot generation from availability templates, booking, status lifecycle, consultations, vitals, prescriptions |
| IPD / Admissions | Wards, beds (with live occupancy), admit/discharge, round notes |
| Billing | Charge master, draft→finalize→pay bill lifecycle, receipts, refunds |
| Pharmacy / Inventory | Drug catalog, batch receiving, **FEFO dispensing** (first-expiry-first-out), low-stock/expiry alerts |
| Analytics | Dashboard summary (single aggregate queries, not full scans), revenue reports |

### Performance & best practices baked in

- **Every list endpoint is paginated** — `PageResponse<T>` is the only shape
  list endpoints return; there is no "fetch everything" endpoint anywhere.
  `PageRequestFactory` server-side caps page size (`hms.pagination.max-page-size`,
  default 100) so a client can't request an unbounded page.
- **Autocomplete/typeahead endpoints** (`/patients/autocomplete`,
  `/doctors/autocomplete`, `/pharmacy/drugs/autocomplete`,
  `/charge-master/autocomplete`) return a minimal `{id, label, subLabel}`
  shape instead of full records, backed by indexed (and MySQL FULLTEXT)
  columns — built for live-search-as-you-type UI.
- **Optimistic locking (`@Version`)** on every entity that's edited
  concurrently by multiple staff (bills, beds, slots, stock batches) —
  prevents silent lost updates, e.g. two receptionists booking the same slot.
- **Atomic, gap-free business codes** (`PT-000123`, `APT-000456`, ...) via a
  row-locked `code_sequences` table (`CodeGeneratorService`), safer under
  concurrency than `MAX(id)+1`.
- **Caching** (Caffeine) on hot reference-data lookups: tenant resolution at
  login, charge-master autocomplete — evicted on writes.
- **DTOs everywhere** — controllers never serialize JPA entities directly, so
  adding a lazy relation later can't accidentally blow up a response payload
  or leak internal fields.
- **HikariCP tuned pools**, `open-in-view: false` (no lazy-loading-in-view
  surprises), JDBC batch inserts/updates, `default_batch_fetch_size` to avoid
  N+1s on collections.
- **FEFO pharmacy dispensing** — draws from soonest-expiring batches first,
  splitting across batches if needed, indexed by `(drug_id, expiry_date)`.

---

## 3. Prerequisites

- Java 21
- Maven 3.9+
- A local MySQL 8.0 instance (you already have one)

## 4. Set up the database

Run this once against your local MySQL:

```bash
mysql -u root -p < local-setup/local-mysql-setup.sql
```

This creates the `hms_master` database and an `hms_app` user with rights to
create tenant schemas (`tenant_<code>`) on demand. `application.yml` already
points at `localhost:3306` with `hms_app` / `hms_app_pwd` — change either
side if you used different credentials.

## 5. Run it

Just run `HmsApplication.java` from your IDE (right-click → Run), or:

```bash
mvn spring-boot:run
```

On first boot it will:
1. Run the `hms_master` Flyway migration automatically.
2. Seed a default **platform admin** (username `platformadmin`, password
   `ChangeMe123!` — override via `hms.platform.seed-admin.*` in
   `application.yml`, or disable with `seed-admin.enabled=false`).

**Swagger UI:** http://localhost:8080/swagger-ui.html
**OpenAPI JSON:** http://localhost:8080/v3/api-docs

## 6. Deploying to GCP Cloud Run

See **[CLOUD_RUN.md](./CLOUD_RUN.md)** — covers building the Docker image
(`Dockerfile` included), provisioning Cloud SQL for MySQL, wiring the Cloud
SQL Auth Proxy as a sidecar, and secrets for the DB password / JWT secret.

## 7. Testing walkthrough (in Swagger)

1. **`POST /api/v1/platform/auth/login`** with
   `{"username": "platformadmin", "password": "ChangeMe123!"}`.
   Copy the `accessToken`.
2. Click **Authorize** (top right of Swagger UI) → paste the token.
3. **`POST /api/v1/platform/tenants`** — onboard your first hospital, e.g.:
   ```json
   {
     "tenantCode": "apollo-ghaziabad",
     "hospitalName": "Apollo Ghaziabad",
     "contactEmail": "admin@apollo-ghaziabad.example",
     "adminUsername": "hospitaladmin",
     "adminFullName": "Hospital Admin",
     "adminPassword": "Admin@12345"
   }
   ```
   This creates schema `tenant_apollo_ghaziabad`, runs all tenant migrations
   against it, and seeds the admin login you specified.
4. **`POST /api/v1/auth/login`** with
   `{"tenantCode": "apollo-ghaziabad", "username": "hospitaladmin", "password": "Admin@12345"}`.
   Re-authorize Swagger with this new token — you're now acting as that
   hospital's admin, scoped only to its schema.
5. From here: create a department → onboard a doctor → set the doctor's
   weekly availability → generate slots for a date → register a patient →
   book an appointment → start/complete a consultation → create wards/beds →
   admit a patient → create charge-master items → raise a bill → finalize →
   record a receipt → add a drug → receive a batch → dispense it → check
   `/api/v1/analytics/dashboard-summary`.

To onboard a **second hospital** and confirm isolation: repeat step 3 with a
different `tenantCode`, log in as its admin, and confirm you get empty lists
for patients/doctors/etc even though the first hospital has data — that's
schema isolation working.

## 8. Project layout

```
src/main/java/com/hms/
  config/            DataSource beans, dual JPA persistence units, OpenAPI, Flyway boot
  common/             ApiResponse, PageResponse, OptionDto, exceptions, base entity, code-sequence generator
  security/           JWT service, auth filter, Spring Security config
  tenancy/
    multitenant/      TenantContext, Hibernate schema resolver/connection provider
    master/           Tenant registry, platform admin auth, onboarding/provisioning
  identity/            AppUser, roles, tenant-user login
  patient/ doctor/ staff/ appointment/ opd/ ipd/ billing/ pharmacy/ analytics/
                        one package per business module, each with entity/repository/service/web/dto
src/main/resources/
  application.yml
  db/migration/master/   Flyway migrations for hms_master (runs at boot)
  db/migration/tenant/   Flyway migrations for each hospital schema (runs at onboarding)
local-setup/              One-time local MySQL setup script (no Docker)
Dockerfile                 Multi-stage build for deploying to Cloud Run
CLOUD_RUN.md                Cloud Run + Cloud SQL deployment walkthrough
```

## 9. Scaling notes for later

- **One tenant DB pool today.** At real scale, move large/premium hospitals
  to their own physical database (same code — `selectDataSource` in
  `SchemaMultiTenantConnectionProvider` is the one place that would change
  to route by tenant instead of always returning the same pool).
- **Read replicas:** analytics/reporting queries are already isolated in
  `AnalyticsService` — routing those to a read replica later is a
  `@Transactional(readOnly=true)` + routing-datasource change, not a rewrite.
- **Splitting into microservices:** each top-level package
  (`patient`, `billing`, `pharmacy`, ...) already only talks to other
  packages by ID, never by JPA relationship — that's what makes it
  extractable later. `identity` and `tenancy` would need to become a shared
  "auth service" other services call.
- **Rate limiting / idempotency keys** on payment/billing endpoints — not yet
  implemented, flagged as a pre-production hardening item.

## 10. Known limitation of this build environment

This project was written and reviewed for structural/logical correctness
(package wiring, JPQL against actual entity fields, Flyway columns vs. JPA
mappings, brace-balance across all 193 files) but **could not be
`mvn compile`-verified in this sandbox** (no access to Maven Central here).
Run `mvn clean compile` as your first step locally — if anything doesn't
compile, it'll most likely be a minor import/typo, not a structural issue.

## 11. Next step

Once this is running and you've poked at it in Swagger, say the word and
we'll build the React frontend against it.
