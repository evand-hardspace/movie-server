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
| C1 | `[x]` | Swap H2/R2DBC → PostgreSQL JDBC in `libs.versions.toml` | Add `postgresql = "42.7.4"` |
| C2 | `[x]` | Update `build.gradle.kts` — remove R2DBC/H2, add `exposed-jdbc` + `postgresql` | |
| C3 | `[x]` | Update `application.yaml` — add `jwt` config block, fix module paths to `plugins.*` | |

### Phase 2 — Restructure & Scaffold

| # | Status | Task | Notes |
|---|--------|------|-------|
| C4 | `[x]` | Delete scaffold files: `GreetingService.kt`, `UsersService.kt`, `Exposed.kt` | Done as part of C2 |
| C5 | `[x]` | Create `plugins/` package, move `Http.kt`, `Serialization.kt`, `Security.kt`, `DI.kt` | Update module refs in `application.yaml` |
| C6 | `[x]` | Create domain models: `Genre.kt` (enum), `Movie.kt`, `User.kt` | Under `domain/model/` |
| C7 | `[x]` | Create Exposed tables: `MoviesTable.kt`, `UsersTable.kt` | Under `domain/table/` |

### Phase 3 — Database & Auth Plugin

| # | Status | Task | Notes |
|---|--------|------|-------|
| C8 | `[x]` | Write `plugins/Database.kt` — JDBC `Database.connect()` from env vars | Replaces `Exposed.kt` |
| C9 | `[x]` | Rewrite `plugins/Security.kt` — Supabase JWKS JWT validation | Read issuer/JWKS URI from config |

### Phase 4 — Services

| # | Status | Task | Notes |
|---|--------|------|-------|
| C10 | `[x]` | Write `domain/service/UserService.kt` — upsert user from JWT claims | |
| C11 | `[x]` | Write `domain/service/MovieService.kt` — CRUD + genre filter | |

### Phase 5 — Routes

| # | Status | Task | Notes |
|---|--------|------|-------|
| C12 | `[x]` | Write `routes/AuthRoutes.kt` — `POST /auth/sync` | |
| C13 | `[x]` | Write `routes/MovieRoutes.kt` — `GET /movies`, `GET /movies/{id}`, `POST /movies`, `PUT /movies/{id}` | |
| C14 | `[x]` | Write `util/PrincipalExt.kt` — helpers to extract user ID + admin flag from JWT principal | |
| C15 | `[x]` | Update `Routing.kt` — remove placeholder routes, wire `AuthRoutes` + `MovieRoutes` | |
| C16 | `[x]` | Update `DI.kt` — provide `UserService` and `MovieService` via Koin | |

### Phase 6 — Favorites

| # | Status | Task | Notes |
|---|--------|------|-------|
| U9 | `[x]` | Run `user_favorites` migration in Supabase SQL editor | Applied via MCP |
| C19 | `[x]` | Create `FavoritesTable.kt` | Under `domain/table/` |
| C20 | `[x]` | Write `domain/service/FavoriteService.kt` — add/remove/list favorites | |
| C21 | `[x]` | Write `routes/FavoriteRoutes.kt` — `POST /movies/{id}/favorite`, `DELETE /movies/{id}/favorite` | Requires auth |
| C22 | `[x]` | Extend `GET /movies` + `GET /movies/{id}` — include `is_favorited` when JWT present | |
| C23 | `[x]` | Wire `FavoriteService` in `DI.kt`, add routes to `Routing.kt` | |

**Schema for U9:**
```sql
CREATE TABLE user_favorites (
    user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    movie_id   UUID NOT NULL REFERENCES movies(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, movie_id)
);
```

---

### Phase 8 — Admin Panel Backend Support

Backend endpoints needed by the admin panel. Both require authentication.

| # | Status | Task | Notes |
|---|--------|------|-------|
| B1 | `[x]` | Add `GET /users/me` — returns current user's id, email, role | Used by admin panel to know the caller's role after login |
| B2 | `[x]` | Add `GET /users` (super_admin only) — returns list of all users with id, email, role | Used by user management tab |

---

### Phase 9 — Admin Panel Module

Separate KMP module `admin-panel/` in this repo. WASM is the only target for now; architecture must support adding targets (Android, Desktop, iOS) without restructuring.

**Multi-target design rules:**
- All UI and business logic lives in `commonMain`
- Platform-specific code (token storage, HTTP engine) isolated behind `expect/actual`
- One `expect class TokenStorage` — `wasmJsMain` uses `localStorage`; future targets add their own `actual`
- Ktor engine selected per source set (`ktor-client-js` for `wasmJsMain`)

#### AP0 — Module Scaffold

| # | Status | Task | Notes |
|---|--------|------|-------|
| AP1 | `[x]` | Create `admin-panel/` module, include it in root `settings.gradle.kts` | KMP module, no Android plugin |
| AP2 | `[x]` | Configure `admin-panel/build.gradle.kts` — wasmJs target, Compose MP plugin, Ktor client, kotlinx-serialization | Compose MP 1.11.0-beta03 with Kotlin 2.3.20 |
| AP3 | `[x]` | `wasmJsMain/main.kt` — WASM entry point calling `ComposeViewport { App() }` | Uses ComposeViewport (replaces CanvasBasedWindow in CMP 1.9+) |
| AP4 | `[x]` | `expect class TokenStorage` in `commonMain`; `actual` in `wasmJsMain` backed by `localStorage` | Isolates storage so adding Android/Desktop needs only a new `actual` |

#### AP1 — Data Layer (commonMain)

| # | Status | Task | Notes |
|---|--------|------|-------|
| AP5 | `[x]` | `ApiClient` — Ktor `HttpClient` wrapper that injects `Authorization: Bearer` from `TokenStorage`; returns typed results or error | Common to all screens |
| AP6 | `[x]` | `AuthRepository` — `login(email, password)` calling `POST /auth/login`; stores tokens via `TokenStorage` | On 401 from any other call, clear storage and redirect to login |
| AP7 | `[x]` | `MovieRepository` — `getMovies()`, `createMovie(request)`, `updateMovie(id, request)` | Maps API DTOs to domain models |
| AP8 | `[x]` | `UserRepository` — `getMe()` calling `GET /users/me`, `getUsers()` calling `GET /users`, `updateRole(id, role)` | `getMe()` used to determine role after login |

#### AP2 — Navigation & Session (commonMain)

| # | Status | Task | Notes |
|---|--------|------|-------|
| AP9 | `[x]` | `AppState` — holds `currentScreen` (sealed class), `userRole`, `accessToken`; drives top-level `App` composable | Simple state machine instead of a nav library — keeps it readable |
| AP10 | `[x]` | `App.kt` — root composable that switches screens based on `AppState`; shows bottom tabs (Movies / Users) for super_admin | Users tab only visible when `role == super_admin` |

#### AP3 — Screens (commonMain)

| # | Status | Task | Notes |
|---|--------|------|-------|
| AP11 | `[x]` | `LoginScreen` — email/password form, calls `AuthRepository.login()`, then `UserRepository.getMe()` to load role | Show error on wrong credentials |
| AP12 | `[x]` | `MovieListScreen` — shows all movies in a list/grid; FAB to add; tap row to edit | Calls `MovieRepository.getMovies()` |
| AP13 | `[x]` | `MovieFormScreen` — single screen for both add and edit; pre-fills fields when editing | Genre shown as dropdown; submit calls `createMovie` or `updateMovie` depending on mode |
| AP14 | `[x]` | `UserListScreen` — shows all users with email + role badge; tap row to change role via dropdown | Only reachable for super_admin; calls `UserRepository.getUsers()` + `updateRole()` |

---

### Phase 7 — Deployment

| # | Status | Task | Notes |
|---|--------|------|-------|
| C17 | `[x]` | Write `Dockerfile` — fat JAR build + `eclipse-temurin:21-jre` runtime | Done |
| C18 | `[x]` | Write `.github/workflows/deploy.yml` — build → push → Cloud Run deploy | `workflow_dispatch` only |
