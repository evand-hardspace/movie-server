# Movie Server — Backlog

Tracks all remaining work. Two sections: **You** (setup/external actions only you can do) and **Code** (implementation handled by Claude).

Status legend: `[ ]` pending · `[~]` in progress · `[x]` done · `[!]` blocked (waiting on dependency)

---

## You (Setup & External)

| # | Status | Task | Notes |
|---|--------|------|-------|
| U1 | `[x]` | Create Supabase project | Project ref: `hrecjmjmncoiydjopnzc` |
| U2 | `[x]` | ~~Enable Google OAuth provider~~ — using Supabase email auth instead | Email auth is enabled by default; no setup needed |
| U3 | `[x]` | Run database schema SQL in Supabase SQL editor | Applied via migration `initial_schema` |
| U4 | `[x]` | Create Supabase Storage bucket for movie images | Bucket `movies` created (public) |
| U5 | `[x]` | Provide environment variable values | All env vars available |
| U6 | `[x]` | Create Google Cloud project | For Cloud Run hosting |
| U7 | `[x]` | Create Google Artifact Registry repository | `us-central1-docker.pkg.dev/$GCP_PROJECT_ID/movie-server` |
| U8 | `[x]` | Set up GitHub Actions secrets (for CI/CD) | WIF auth; 8 secrets set |

**Required env vars (for U5):**
- `DATABASE_URL` — `jdbc:postgresql://db.hrecjmjmncoiydjopnzc.supabase.co:5432/postgres`
- `DATABASE_USER` — `postgres`
- `DATABASE_PASSWORD` — **needed from you**: Supabase dashboard → Settings → Database
- `SUPABASE_JWT_ISSUER` — `https://hrecjmjmncoiydjopnzc.supabase.co/auth/v1`
- `SUPABASE_JWKS_URI` — `https://hrecjmjmncoiydjopnzc.supabase.co/auth/v1/keys`

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
