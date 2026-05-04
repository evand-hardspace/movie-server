# Movie Server — CLAUDE.md

## Project Overview

Ktor backend for managing a movie catalogue. Features self-owned email/password auth (HMAC256 JWT), movie CRUD with photo storage, genre-based filtering, favorites, and an admin role that gates write operations. All data stored in SQLite.

This project serves **educational purposes** as a simple Ktor backend for an Android application. The target audience is Android students learning how to build and connect a real backend.

---

## Tech Stack

| Layer | Choice | Notes |
|---|---|---|
| Framework | Ktor 3.4.0 (Netty) | |
| Language | Kotlin 2.3.20, JVM 21 | |
| Auth | Self-owned (BCrypt + HMAC256 JWT) | Server issues and validates its own JWTs |
| Database | SQLite (`org.xerial:sqlite-jdbc`) | File-based; path via `SQLITE_FILE` env var |
| ORM | JetBrains Exposed 1.2.0 | JDBC; `SchemaUtils.create` on startup |
| Storage | Supabase Storage | Client uploads directly; Ktor stores URL |
| DI | Ktor `ktor-server-di` | |
| Serialization | kotlinx.serialization | |
| Hosting | Google Cloud Run | Dockerized fat JAR |
| Build | Gradle Kotlin DSL | Version catalog in `gradle/libs.versions.toml` |

---

## Architecture

```
Client
  │
  ├─ 1. POST /auth/register or /auth/login → receives JWT + refresh token
  ├─ 2. Upload photo → Supabase Storage (presigned URL) → receives photo_url
  │
  ▼
Cloud Run (Ktor)
  ├─ Validate HMAC256 JWT (JWT_SECRET)
  ├─ Extract user ID + email from JWT claims
  ├─ JDBC → SQLite (Exposed DSL)
  └─ Returns/persists photo_url (no binary traffic through Ktor)
```

**Photo upload flow:** Client requests a presigned upload URL from Supabase Storage directly, uploads the file, then sends the resulting public `photo_url` to the Ktor API. Ktor never handles binary data.

**SQLite persistence on Cloud Run:** Container storage is ephemeral — data resets on every redeploy. Mount a persistent volume at `SQLITE_FILE` for production. Use `SEED_ADMIN_EMAIL` + `SEED_ADMIN_PASSWORD` env vars to auto-create a super_admin and seed 10 sample movies on first boot (only runs when the users/movies tables are empty).

---

## Database Schema

Managed by Exposed `SchemaUtils.create(...)` on startup (no external migrations needed).

```
users          — id (UUID/TEXT), email, password_hash, role (TEXT enum), created_at (epoch ms)
refresh_tokens — token (TEXT PK), user_id (FK), expires_at (epoch ms), is_revoked (bool)
movies         — id (UUID/TEXT), title, description, genre (TEXT enum), rating, photo_url,
                 created_by (FK), created_at (epoch ms), updated_at (epoch ms)
user_favorites — user_id + movie_id (composite PK), created_at (epoch ms)
```

---

## API Design

All endpoints return `application/json`. Auth header: `Authorization: Bearer <jwt>`.

### Auth
| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/auth/register` | None | Create account; returns tokens |
| `POST` | `/auth/login` | None | Sign in; returns tokens |
| `POST` | `/auth/refresh` | None | Exchange refresh token for new access token |

### Movies
| Method | Path | Auth | Description |
|---|---|---|---|
| `GET` | `/movies` | Optional | List all movies (array); filter via `?genre=ACTION`; includes `is_favorited` when JWT present |
| `GET` | `/movies?page=1&page_size=20` | Optional | Paginated movies; same filters apply; returns `PagedMoviesResponse` object |
| `GET` | `/movies/{id}` | Optional | Get single movie; includes `is_favorited` when JWT present |
| `POST` | `/movies` | Admin | Create movie |
| `PUT` | `/movies/{id}` | Admin | Update movie |
| `DELETE` | `/movies/{id}` | Admin | Delete movie |

### Favorites
| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/movies/{id}/favorite` | Required | Add movie to favorites |
| `DELETE` | `/movies/{id}/favorite` | Required | Remove movie from favorites |

### Users
| Method | Path | Auth | Description |
|---|---|---|---|
| `GET` | `/users/me` | Required | Current user's id, email, role |
| `GET` | `/users` | super_admin | List all users with id, email, role |
| `PUT` | `/users/{id}/role` | super_admin | Update a user's role |

### Key request/response shapes

**POST /auth/register, POST /auth/login**
```json
{ "email": "user@example.com", "password": "secret" }
```
Response:
```json
{ "access_token": "...", "refresh_token": "...", "token_type": "Bearer", "expires_in": 3600 }
```

**POST /auth/refresh**
```json
{ "refresh_token": "uuid-string" }
```

**GET /movies response** (full list)
```json
[{ "id": "uuid", "title": "Inception", "genre": "THRILLER", "rating": 8.8, "is_favorited": false, "created_at": "..." }]
```

**GET /movies?page=1&page_size=20 response** (paginated)
```json
{
  "items": [{ "id": "uuid", "title": "Inception", "genre": "THRILLER", "rating": 8.8, "is_favorited": false, "created_at": "..." }],
  "page": 1,
  "page_size": 20,
  "total": 42,
  "total_pages": 3
}
```
`page_size` is clamped to 1–100 (default 20). `page` is 1-based.

---

## Auth Flow

1. Client calls `POST /auth/register` or `POST /auth/login` with email + password
2. Server verifies credentials (BCrypt), issues HMAC256 access JWT (1-hour TTL) + opaque refresh token (30-day TTL)
3. Refresh token stored in `refresh_tokens` table; revoked on use (rotation)
4. Subsequent requests carry `Authorization: Bearer <access_token>`
5. Roles: `USER` (default), `ADMIN` (write access to movies), `SUPER_ADMIN` (user management)
6. First-boot seed: set `SEED_ADMIN_EMAIL` + `SEED_ADMIN_PASSWORD` to auto-create a super_admin (if no users exist) and seed 10 sample movies (if movies table is empty)

**JWT config (`application.yaml`):**
```yaml
jwt:
  secret: ${JWT_SECRET}
  audience: "authenticated"
```

---

## Project Structure

```
movie-server/
├── server/                          # Ktor backend module
│   └── src/main/kotlin/com/evandhardspace/movie/server/
│       ├── main.kt
│       ├── Routing.kt
│       ├── plugins/
│       │   ├── Http.kt              # CORS, caching headers
│       │   ├── Serialization.kt
│       │   ├── Security.kt          # HMAC256 JWT validation
│       │   ├── DI.kt                # Service wiring
│       │   ├── Database.kt          # SQLite connection + SchemaUtils.create
│       │   └── Seed.kt              # Optional super_admin + movies seed on first boot
│       ├── routes/
│       │   ├── AuthRoutes.kt        # POST /auth/register, /login, /refresh
│       │   ├── MovieRoutes.kt       # GET+POST+PUT+DELETE /movies
│       │   ├── FavoriteRoutes.kt    # POST+DELETE /movies/{id}/favorite
│       │   └── UserRoutes.kt        # GET /users/me, GET /users, PUT /users/{id}/role
│       ├── domain/
│       │   ├── model/               # Movie, Genre, User, UserRole
│       │   ├── table/               # UsersTable, RefreshTokensTable, MoviesTable, FavoritesTable
│       │   └── service/             # AuthService, UserService, MovieService, FavoriteService
│       └── util/
│           └── PrincipalExt.kt      # userId(), userEmail() helpers
└── admin-panel/                     # KMP admin panel module (WASM + JVM targets)
    └── src/
        ├── commonMain/              # all UI + business logic
        │   └── .../adminpanel/
        │       ├── App.kt / AppState.kt / Config.kt
        │       ├── data/            # ApiClient, AuthRepository, MovieRepository, UserRepository
        │       ├── domain/model/    # Movie, Genre, User, UserRole
        │       ├── screen/          # LoginScreen, MovieListScreen, MovieFormScreen, UserListScreen
        │       └── storage/         # expect class TokenStorage
        ├── wasmJsMain/              # actual TokenStorage (localStorage), main entry
        └── jvmMain/                 # actual TokenStorage, main entry
```

---

## Admin Panel

Separate KMP module (`admin-panel/`) targeting WASM and JVM. Architecture supports adding Android/Desktop/iOS without restructuring.

**Multi-target design:**
- All UI and business logic in `commonMain`
- `expect class TokenStorage` — `wasmJsMain` uses `localStorage`; `jvmMain` for desktop/testing
- Ktor client engine selected per source set

**Screens:** Login → Movie list (FAB to add, tap to edit) → Movie form (create/edit) → User list (super_admin only, role management)

---

## Environment Variables

| Variable | Required | Description |
|---|---|---|
| `JWT_SECRET` | Yes | HMAC256 signing key (min 32 bytes) |
| `SQLITE_FILE` | No | SQLite file path (default: `./movies.db`) |
| `SEED_ADMIN_EMAIL` | No | Auto-create super_admin on first boot if set |
| `SEED_ADMIN_PASSWORD` | No | Password for the seeded super_admin |
| `GCP_PROJECT_ID` | Deploy | Google Cloud project ID — device only, never committed |

---

## Deployment

- **Containerize:** `./gradlew :server:buildFatJar` → `Dockerfile` FROM `eclipse-temurin:21-jre`
- **Registry:** Google Artifact Registry — `us-central1-docker.pkg.dev/$GCP_PROJECT_ID/movie-server`
- **Deploy:** `gcloud run deploy movie-server --image ... --set-env-vars="JWT_SECRET=...,SQLITE_FILE=/data/movies.db"`
- **CI:** GitHub Actions → test → build → push → deploy (`workflow_dispatch`)
- **Persistence:** SQLite file is ephemeral on Cloud Run by default. Mount a volume at `SQLITE_FILE` for data persistence across deploys.

---

## Decisions Log

| Decision | Choice | Reason |
|---|---|---|
| Auth | Self-owned BCrypt + HMAC256 JWT | No external dependency; full ownership of auth stack |
| Token storage | SQLite refresh_tokens table (opaque, rotated) | Simple revocation; no Redis needed |
| Database | SQLite | Zero-config, self-contained, no external service |
| Photo upload | Client → Supabase Storage directly | Keeps Ktor stateless; no binary traffic |
| Genre / Role | Kotlin enum stored as TEXT | SQLite has no native enum type |
| Timestamps | epoch ms (long) | Avoids JDBC dialect differences for date types |
| DB access | Exposed DSL over JDBC | Idiomatic Kotlin; sync JDBC simple for this scale |
| Rating scale | 0.0–10.0 NUMERIC(3,1) | Industry standard range |
| Hosting | Cloud Run | Scales to zero; no cluster ops |
| Admin panel | KMP WASM module in same repo | Reuses domain models; single deploy artifact |
| Pagination | Query param `?page=` on existing `/movies` | Preserves backwards compatibility; absence of param returns full list |
| Admin panel pagination | Infinite scroll (`LazyListState` + `derivedStateOf`) | No extra UI chrome; natural mobile/web pattern |
| Movie seed | 10 hardcoded movies across all genres | Covers every `Genre` enum value; seeded under admin's UUID as `created_by` |
