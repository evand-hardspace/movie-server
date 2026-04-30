# Movie Server — Backlog

Tracks all remaining work. Two sections: **You** (setup/external actions only you can do) and **Code** (implementation handled by Claude).

Status legend: `[ ]` pending · `[~]` in progress · `[x]` done · `[!]` blocked (waiting on dependency)

---

## You (Setup & External)

| # | Status | Task | Notes |
|---|--------|------|-------|
| U1 | `[ ]` | Create Supabase project | Needed before any backend config |
| U2 | `[ ]` | Enable Google OAuth provider in Supabase Auth dashboard | Auth → Providers → Google |
| U3 | `[!]` | Run database schema SQL in Supabase SQL editor | Blocked on U1; SQL is in CLAUDE.md under "Database Schema" |
| U4 | `[!]` | Create Supabase Storage bucket for movie images | Blocked on U1; set bucket to public or use signed URLs |
| U5 | `[!]` | Provide environment variable values | Blocked on U1; see list below |
| U6 | `[ ]` | Create Google Cloud project | For Cloud Run hosting |
| U7 | `[!]` | Create Google Artifact Registry repository | Blocked on U6 |
| U8 | `[!]` | Set up GitHub Actions secrets (for CI/CD) | Blocked on U5, U7 |

**Required env vars (for U5):**
- `DATABASE_URL` — `jdbc:postgresql://db.<ref>.supabase.co:5432/postgres`
- `DATABASE_USER` — `postgres`
- `DATABASE_PASSWORD` — from Supabase dashboard → Settings → Database
- `SUPABASE_JWT_ISSUER` — `https://<ref>.supabase.co/auth/v1`
- `SUPABASE_JWKS_URI` — `https://<ref>.supabase.co/auth/v1/keys`

---

## Code (Claude)

### Phase 1 — Dependencies & Config

| # | Status | Task | Notes |
|---|--------|------|-------|
| C1 | `[ ]` | Swap H2/R2DBC → PostgreSQL JDBC in `libs.versions.toml` | Add `postgresql = "42.7.4"` |
| C2 | `[ ]` | Update `build.gradle.kts` — remove R2DBC/H2, add `exposed-jdbc` + `postgresql` | |
| C3 | `[ ]` | Update `application.yaml` — add `jwt` config block, fix module paths to `plugins.*` | |

### Phase 2 — Restructure & Scaffold

| # | Status | Task | Notes |
|---|--------|------|-------|
| C4 | `[ ]` | Delete scaffold files: `GreetingService.kt`, `UsersService.kt`, `Exposed.kt` | Clean slate |
| C5 | `[ ]` | Create `plugins/` package, move `Http.kt`, `Serialization.kt`, `Security.kt`, `DI.kt` | Update module refs in `application.yaml` |
| C6 | `[ ]` | Create domain models: `Genre.kt` (enum), `Movie.kt`, `User.kt` | Under `domain/model/` |
| C7 | `[ ]` | Create Exposed tables: `MoviesTable.kt`, `UsersTable.kt` | Under `domain/table/` |

### Phase 3 — Database & Auth Plugin

| # | Status | Task | Notes |
|---|--------|------|-------|
| C8 | `[ ]` | Write `plugins/Database.kt` — JDBC `Database.connect()` from env vars | Replaces `Exposed.kt` |
| C9 | `[ ]` | Rewrite `plugins/Security.kt` — Supabase JWKS JWT validation | Read issuer/JWKS URI from config |

### Phase 4 — Services

| # | Status | Task | Notes |
|---|--------|------|-------|
| C10 | `[ ]` | Write `domain/service/UserService.kt` — upsert user from JWT claims | |
| C11 | `[ ]` | Write `domain/service/MovieService.kt` — CRUD + genre filter | |

### Phase 5 — Routes

| # | Status | Task | Notes |
|---|--------|------|-------|
| C12 | `[ ]` | Write `routes/AuthRoutes.kt` — `POST /auth/sync` | |
| C13 | `[ ]` | Write `routes/MovieRoutes.kt` — `GET /movies`, `GET /movies/{id}`, `POST /movies`, `PUT /movies/{id}` | |
| C14 | `[ ]` | Write `util/PrincipalExt.kt` — helpers to extract user ID + admin flag from JWT principal | |
| C15 | `[ ]` | Update `Routing.kt` — remove placeholder routes, wire `AuthRoutes` + `MovieRoutes` | |
| C16 | `[ ]` | Update `DI.kt` — provide `UserService` and `MovieService` via Koin | |

### Phase 6 — Deployment

| # | Status | Task | Notes |
|---|--------|------|-------|
| C17 | `[!]` | Write `Dockerfile` — fat JAR build + `eclipse-temurin:21-jre` runtime | Blocked on C1–C16 |
| C18 | `[!]` | Write `.github/workflows/deploy.yml` — build → push → Cloud Run deploy | Blocked on U7, U8, C17 |
