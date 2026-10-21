# Loan Origination Portal

A server-rendered small-business loan origination portal built with **Java 21, Quarkus, Qute templates, and Hibernate ORM with Panache** on PostgreSQL. Single-module monolith, no frontend framework.

## Quick start (Docker)

```bash
cp .env.example .env   # optional - defaults work out of the box
docker compose up --build
```

This builds the app, starts Postgres, runs Flyway migrations, loads seed data, and serves the app at **http://localhost:58080** (Postgres is exposed on `55432` if you want to inspect it directly — both ports are non-default to avoid colliding with other local services).

## Local development (without Docker)

Requires JDK 21 and a local PostgreSQL instance.

```bash
createuser -P loanportal          # password: loanportal_dev_pw
createdb -O loanportal loanportal
export DB_URL=jdbc:postgresql://localhost:5432/loanportal
export DB_USER=loanportal
export DB_PASSWORD=loanportal_dev_pw
export SESSION_SECRET=dev-only-session-secret-change-me-please-32bytes
./gradlew quarkusDev
```

The app listens on `http://localhost:8080`.

## Running tests

```bash
createdb -O loanportal loanportal_test
export DB_URL=jdbc:postgresql://localhost:5432/loanportal_test
export DB_USER=loanportal
export DB_PASSWORD=loanportal_dev_pw
./gradlew test
```

`./gradlew build -x test` compiles and packages the app; `./gradlew build` additionally runs the test suite above (a reachable Postgres test database is required).

## Demo credentials

All seeded accounts share the passwords below. **Change these before any non-local use.**

| Role               | Email                              | Password          |
|--------------------|-------------------------------------|--------------------|
| Admin              | `admin@loanportal.test`             | `Admin!12345`      |
| Underwriter        | `uw.morgan@loanportal.test`         | `Underwrite!123`   |
| Underwriter        | `uw.priya@loanportal.test`          | `Underwrite!123`   |
| Support specialist | `support.dana@loanportal.test`      | `Support!1234`     |
| Borrower           | `owner@brightleafbakery.test`       | `Borrower!123`     |

12 more seeded borrower accounts exist (`owner@<business>.test`), all sharing the `Borrower!123` password, spanning every application status, missing/rejected/expired documents, counteroffers, approvals, and rejections.

## Trust boundary / tier statement

This is a **single deployable module** (the app spec marks the support-module stack as "n/a"). There is no separate API gateway or decision service: the same Quarkus process that terminates the browser session (form-based, signed session cookie) also makes every authorization decision (`@RolesAllowed` plus explicit ownership checks in `OwnershipService`) and every data-access decision (Panache/Hibernate against Postgres). It is therefore **both the channel and the decision point** — there is no lower-trust edge tier to delegate access checks to, so every request is authenticated and authorized in-process, on every request, with no implicit trust granted by network position.

## Architecture

```
src/main/java/com/loanportal/
  model/       JPA entities + the two explicit state machines (ApplicationStatus, DocumentStatus)
  dto/         Form-binding DTOs (never entities) for every POST endpoint
  service/     Business logic: ownership checks, workflow transitions, audit, decisions, documents, CSV import/export
  security/    Custom form-based session auth (signed cookie, BCrypt), CSRF guard, CurrentUser accessor
  web/         JAX-RS resources + @CheckedTemplate template bindings, one package per audience
  scheduler/   @Scheduled background job (document expiration sweep)
  util/        CSV helpers, the two intentionally non-cryptographic precision-baseline utilities
src/main/resources/
  db/migration/     Flyway migrations (schema + seed data)
  templates/         Qute .html templates, one folder per audience (auth, borrower, underwriter, admin, shared)
  META-INF/resources/static/  Custom CSS design system + minimal vanilla JS
```

### Authentication & sessions

Login is form-based (`POST /login`) and issues a signed, time-bound session cookie (HMAC-SHA256, `SessionTokenService`) — not a framework default. A custom `HttpAuthenticationMechanism` (`SessionAuthMechanism`) verifies the cookie signature/expiry on every request and hands a `TrustedAuthenticationRequest` to `LoanPortalIdentityProvider`, which loads the `User` and builds the `SecurityIdentity` used by `@RolesAllowed`. Passwords are hashed with BCrypt (`io.quarkus.elytron.security.common.BcryptUtil`). Password reset uses a single-use, time-bound, SHA-256-hashed token (`PasswordResetToken`) delivered via the mock email outbox.

### Authorization

Every controller method is annotated `@RolesAllowed`, **and** every record access additionally goes through `OwnershipService`, which is called one call frame above the controller (in `ApplicationWorkflowService`/`DecisionService`/resource methods) rather than inline in each controller — this is intentional, see `PRECISION_BASELINE.md` item 1. `support_specialist` is blocked from recording decisions **in `DecisionService.decide()`**, not just hidden in the UI (see `DecisionServiceTest`).

### State machines

`ApplicationStatus` and `DocumentStatus` (`model/`) each carry an explicit `Map<Status, Set<Status>>` of allowed transitions, checked by `canTransitionTo()` before every write. A final approval is blocked while any required document is `REQUESTED`, `UPLOADED` (not yet reviewed), `REJECTED`, `NEEDS_RESUBMISSION`, or `EXPIRED` — enforced in `DecisionService.enforceDocumentGate()`.

### Audit

Every state transition and privileged action writes an append-only row via `AuditService.record()` — the only code path that inserts into `audit_event`. No route updates or deletes audit rows. Sensitive reads (application view, document download) each write one audit event per request.

### File uploads

Uploaded documents are stored on local disk under `app.upload.base-dir` (bind-mounted as a Docker volume in compose), named with a random token (never the original filename), with metadata (original filename, content type, size, status) in Postgres. Downloads resolve the stored relative path against the canonicalized base directory (`Path.normalize()` + `toRealPath()` containment check) before serving — see precision baseline item 5.

### Mocked external integrations

Emails write to `outbox_email`, webhook deliveries write to `webhook_log`; both are inspectable via SQL for local testing. There is no real SMTP/webhook/credit-bureau/core-banking integration.

### Background job

`DocumentExpirationJob` (`@Scheduled(every = "6h")`) sweeps `ACCEPTED` documents past their `expiresAt` and flips them to `EXPIRED`, notifying the borrower — a real, non-stub scheduled job.

## UI

Custom design system (`static/css/app.css`) — not default Bootstrap — using a deep slate-navy/teal palette with WCAG-AA-contrast semantic status badges (icon + color + text, never color alone), visible focus rings, accessible form labels, breadcrumbs, empty/error/success states, and a responsive grid that collapses to a single column at tablet width. Nav is role-aware (borrower/underwriter/support/admin each see only their own sections) with a logout control and unread-notification badge in every page's top bar. Screens: login/register/forgot-reset password, borrower dashboard, application list, application wizard (loan request → review/submit), tabbed application detail (overview/documents/messages/decision, shared with a staff variant that adds internal notes/decision panel/audit trail), underwriter queue with sortable/filterable columns, document review, admin screens for lending products/document type templates/users/audit log/CSV import/CSV export, and an in-app notifications page.

## Precision baseline (intentionally benign, non-scoring)

Each is marked with a `non-scoring` code comment at its definition site:

1. **`OwnershipService`** — ownership/scope check performed in a shared service one call frame above the controller.
2. **`NonCryptoUtil.cacheBustSuffix()`** — `java.util.Random` used only for a static-asset cache-busting query string.
3. **`NonCryptoUtil.weakEtag()`** — MD5 used only as a change-detection ETag for the public lending-products reference-data response.
4. **`ApplicationQueryService.SORT_WHITELIST`** — the underwriter queue's `sortBy` parameter is mapped through a fixed whitelist to a real JPQL property before being handed to a parameterized Panache query; never string-concatenated.
5. **`DocumentStorageService.resolveForRead()`** — file downloads resolve the stored path against the canonicalized upload base directory (`normalize()` + `toRealPath()` containment check) before serving.

## CSV export scope

- **Applications export** (`/admin/export/applications`): status, amount, product, assignment — no internal notes, no file contents.
- **Decisions export** (`/admin/export/decisions`): includes decision reasons (staff-facing), never raw uploaded file bytes, never internal underwriting notes.
- **Document status export** (`/admin/export/document-status`): status/metadata only, never file bytes.

No export includes `UnderwritingNote` rows; those are queried only from `underwriter/` resources and rendered only in the staff application-detail template.

## Tests

`src/test/java/com/loanportal/`:
- `model/ApplicationStatusTest`, `model/DocumentStatusTest` — pure unit tests of the state-machine transition tables.
- `service/OwnershipServiceTest` — a borrower cannot read another borrower's application; staff can read any application.
- `service/DecisionServiceTest` — a support specialist cannot record a decision (`ForbiddenException`); approval is blocked while a required document is missing/rejected; approval succeeds once all required documents are accepted.

## Known gaps / cut scope

Being honest about what was simplified under time constraints:

- The application "wizard" is a single loan-request form followed by a review/submit screen, rather than a multi-step business-details → owners → loan-request → review flow. Business details are captured at registration and edited on the profile page; business owners and payment accounts are added from the profile page (`POST /profile/owners`, `POST /profile/payment-accounts`) rather than inside the application wizard itself.
- CSV import supports borrower leads only (as specified); there is no bulk application importer.
- No email/SMS delivery beyond the local outbox table (as specified — mocked).

Everything else in the specification (roles, state machines, document lifecycle, decision panel, audit trail, notifications, password reset, CSV import/export, the five precision-baseline items, Docker/Flyway/seed data, Jenkinsfile, authorization tests) is implemented and was exercised against a running instance during development (login for all four roles, cross-borrower access denial, support-specialist decision block, document upload/accept, CSV import/export, path-resolved download) — see the verification notes in the PR/commit description for the exact commands run.
