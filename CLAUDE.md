# Movie Server — CLAUDE.md

## Project Overview

Ktor backend for managing a movie catalogue. Features email/password auth via Supabase, movie CRUD with photo storage, genre-based filtering, favorites, and an admin role that gates write operations.

This project serves **educational purposes** as a simple Ktor backend for an Android application. The target audience is Android students learning how to build and connect a real backend.

---

## Tech Stack

| Layer | Choice | Notes |
|---|---|---|
| Framework | Ktor 3.4.0 (Netty) | |
| Language | Kotlin 2.3.20, JVM 21 | |
| Auth | Supabase Auth (email/password) | Issues JWTs; Ktor validates them |
| Database | Supabase PostgreSQL | Via JDBC + Exposed DSL |
| ORM | JetBrains Exposed 1.2.0 | JDBC |
| Storage | Supabase Storage | Client uploads directly; Ktor stores URL |
| DI | Koin (`ktor-server-di`) | |
| Serialization | kotlinx.serialization | |
| Hosting | Google Cloud Run | Dockerized fat JAR |
| Build | Gradle Kotlin DSL | Version catalog in `gradle/libs.versions.toml` |

---

## Architecture

```
Client
  │
  ├─ 1. Email/password → Supabase Auth → JWT
  ├─ 2. Upload photo → Supabase Storage (presigned URL) → receives photo_url
  │
  ▼
Cloud Run (Ktor)
  ├─ Validate Supabase JWT (JWKS endpoint)
  ├─ Extract user ID + role claim
  ├─ JDBC → Supabase PostgreSQL (Exposed DSL)
  └─ Returns/persists photo_url (no binary traffic through Ktor)
```

**Photo upload flow:** Client requests a presigned upload URL from Supabase Storage directly, uploads the file, then sends the resulting public `photo_url` to the Ktor API. Ktor never handles binary data.

---

## Database Schema

```sql
CREATE TABLE users (
    id         UUID PRIMARY KEY,  -- matches Supabase auth user ID
    email      TEXT NOT NULL,
    is_admin   BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TYPE genre AS ENUM (
    'ACTION', 'COMEDY', 'DRAMA', 'HORROR',
    'THRILLER', 'ROMANCE', 'SCI_FI', 'DOCUMENTARY', 'ANIMATION'
);

CREATE TABLE movies (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title       TEXT NOT NULL,
    description TEXT,
    genre       genre NOT NULL,
    rating      NUMERIC(3,1) CHECK (rating >= 0 AND rating <= 10),
    photo_url   TEXT,
    created_by  UUID NOT NULL REFERENCES users(id),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_movies_genre ON movies(genre);

CREATE TABLE user_favorites (
    user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    movie_id   UUID NOT NULL REFERENCES movies(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, movie_id)
);
```

---

## API Design

All endpoints return `application/json`. Auth header: `Authorization: Bearer <supabase-jwt>`.

### Auth
| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/auth/sync` | Required | Upsert user record on first login |

### Movies
| Method | Path | Auth | Description |
|---|---|---|---|
| `GET` | `/movies` | Optional | List movies; filter via `?genre=ACTION`; includes `is_favorited` when JWT present |
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

### Request/Response shapes

**POST /movies / PUT /movies/{id}**
```json
{
  "title": "Inception",
  "description": "A mind-bending thriller",
  "genre": "THRILLER",
  "rating": 8.8,
  "photo_url": "https://<project>.supabase.co/storage/v1/object/public/movies/inception.jpg"
}
```

**GET /movies response**
```json
[
  {
    "id": "uuid",
    "title": "Inception",
    "description": "...",
    "genre": "THRILLER",
    "rating": 8.8,
    "photo_url": "https://...",
    "is_favorited": false,
    "created_at": "2026-04-30T10:00:00Z"
  }
]
```

---

## Auth Flow

1. Client signs in with email/password via Supabase Auth SDK
2. Supabase issues a JWT containing `sub` (user UUID), `email`, and app_metadata
3. Ktor validates the JWT using Supabase's JWKS endpoint:
   `https://hrecjmjmncoiydjopnzc.supabase.co/auth/v1/keys`
4. On first login, client calls `POST /auth/sync` — Ktor upserts the user row
5. Roles: `is_admin = true` in `users` table for admin; `super_admin` role for user management

**JWT validation config (application.yaml):**
```yaml
jwt:
  issuer: "https://hrecjmjmncoiydjopnzc.supabase.co/auth/v1"
  audience: "authenticated"
  jwksUri: "https://hrecjmjmncoiydjopnzc.supabase.co/auth/v1/keys"
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
│       │   ├── Security.kt          # JWT validation via Supabase JWKS
│       │   ├── DI.kt                # Koin modules
│       │   └── Database.kt          # Exposed + PostgreSQL connection
│       ├── routes/
│       │   ├── AuthRoutes.kt        # POST /auth/sync
│       │   ├── MovieRoutes.kt       # GET+POST+PUT+DELETE /movies
│       │   ├── FavoriteRoutes.kt    # POST+DELETE /movies/{id}/favorite
│       │   └── UserRoutes.kt        # GET /users/me, GET /users
│       ├── domain/
│       │   ├── model/
│       │   │   ├── Movie.kt
│       │   │   ├── Genre.kt
│       │   │   ├── User.kt
│       │   │   └── UserRole.kt
│       │   ├── table/
│       │   │   ├── MoviesTable.kt
│       │   │   ├── UsersTable.kt
│       │   │   └── FavoritesTable.kt
│       │   └── service/
│       │       ├── MovieService.kt
│       │       ├── UserService.kt
│       │       ├── FavoriteService.kt
│       │       └── AuthService.kt
│       └── util/
│           └── PrincipalExt.kt      # helpers to extract user/admin from ApplicationCall
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

| Variable | Description |
|---|---|
| `DATABASE_URL` | `jdbc:postgresql://db.hrecjmjmncoiydjopnzc.supabase.co:5432/postgres` |
| `DATABASE_USER` | `postgres` |
| `DATABASE_PASSWORD` | Supabase dashboard → Settings → Database |
| `SUPABASE_JWT_ISSUER` | `https://hrecjmjmncoiydjopnzc.supabase.co/auth/v1` |
| `SUPABASE_JWKS_URI` | `https://hrecjmjmncoiydjopnzc.supabase.co/auth/v1/keys` |
| `GCP_PROJECT_ID` | Google Cloud project ID — stored on device only, never committed |

---

## Deployment

- **Containerize:** `./gradlew buildFatJar` → `Dockerfile` FROM `eclipse-temurin:21-jre`
- **Registry:** Google Artifact Registry — `us-central1-docker.pkg.dev/$GCP_PROJECT_ID/movie-server`
- **Deploy:** `gcloud run deploy movie-server --image us-central1-docker.pkg.dev/$GCP_PROJECT_ID/movie-server/movie-server:latest --region us-central1`
- **CI:** GitHub Actions → build → push → deploy (`workflow_dispatch`)

---

## Decisions Log

| Decision | Choice | Reason |
|---|---|---|
| Auth provider | Supabase Auth (email/password) | Same vendor as DB; handles auth flow + JWT issuance |
| Photo upload | Client → Supabase Storage directly | Keeps Ktor stateless; no binary traffic |
| Genre | Kotlin enum + PostgreSQL ENUM | Fixed set; type-safe filtering |
| DB access | Exposed DSL over JDBC | Idiomatic Kotlin; sync JDBC simpler than R2DBC |
| Rating scale | 0.0–10.0 NUMERIC(3,1) | Industry standard range |
| Admin grant | Manual via Supabase dashboard (`is_admin` flag) | Simple for MVP; no promotion endpoint needed |
| Hosting | Cloud Run | Scales to zero; no cluster ops; fits containerized Ktor |
| Admin panel | KMP WASM module in same repo | Reuses domain models; single deploy artifact |
