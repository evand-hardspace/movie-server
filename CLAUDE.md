# Movie Server — CLAUDE.md

## Project Overview

Ktor backend for managing a movie catalogue. Features Google Sign-In via Supabase Auth, movie CRUD with photo storage, genre-based filtering, and an admin role that gates write operations.

---

## Tech Stack

| Layer | Choice | Notes |
|---|---|---|
| Framework | Ktor 3.4.0 (Netty) | Already scaffolded |
| Language | Kotlin 2.3.20, JVM 21 | |
| Auth | Supabase Auth (Google OAuth) | Issues JWTs; Ktor validates them |
| Database | Supabase PostgreSQL | Via JDBC + Exposed DSL |
| ORM | JetBrains Exposed 1.2.0 | Switch from R2DBC → JDBC |
| Storage | Supabase Storage | Client uploads directly; Ktor stores URL |
| DI | Koin (`ktor-server-di`) | Already wired |
| Serialization | kotlinx.serialization | Already wired |
| Hosting | Google Cloud Run | Dockerized fat JAR |
| Build | Gradle Kotlin DSL | Version catalog in `gradle/libs.versions.toml` |

---

## Architecture

```
Client
  │
  ├─ 1. Google OAuth → Supabase Auth → JWT
  ├─ 2. Upload photo → Supabase Storage (presigned URL) → receives photo_url
  │
  ▼
Cloud Run (Ktor)
  ├─ Validate Supabase JWT (JWKS endpoint)
  ├─ Extract user ID + is_admin claim
  ├─ JDBC → Supabase PostgreSQL (Exposed DSL)
  └─ Returns/persists photo_url (no binary traffic through Ktor)
```

**Photo upload flow (Option A — client-direct):** The client requests a presigned upload URL from Supabase Storage directly, uploads the file, then sends the resulting public `photo_url` to the Ktor API. Ktor never handles binary data.

---

## Database Schema

```sql
-- Populated on first authenticated request (synced from Supabase auth.users)
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
| `GET` | `/movies` | Optional | List movies; filter via `?genre=ACTION` |
| `GET` | `/movies/{id}` | Optional | Get single movie |
| `POST` | `/movies` | Admin | Create movie |
| `PUT` | `/movies/{id}` | Admin | Update movie |

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
    "created_at": "2026-04-30T10:00:00Z"
  }
]
```

---

## Auth Flow

1. Client initiates Google OAuth via Supabase Auth SDK
2. Supabase issues a JWT containing `sub` (user UUID), `email`, and app_metadata
3. Ktor validates the JWT using Supabase's JWKS endpoint:
   `https://<project-ref>.supabase.co/auth/v1/keys`
4. On first login, client calls `POST /auth/sync` — Ktor upserts the user row
5. Admin role: `is_admin = true` in the `users` table; checked on each write request

**JWT validation config (application.yaml):**
```yaml
jwt:
  issuer: "https://<project-ref>.supabase.co/auth/v1"
  audience: "authenticated"
  jwksUri: "https://<project-ref>.supabase.co/auth/v1/keys"
```

---

## Project Structure (target)

```
src/main/kotlin/com/evandhardspace/movie/server/
├── main.kt
├── plugins/
│   ├── Http.kt           # CORS, caching headers
│   ├── Serialization.kt
│   ├── Security.kt       # JWT validation via Supabase JWKS
│   ├── DI.kt             # Koin modules
│   └── Database.kt       # Exposed + PostgreSQL connection (replaces Exposed.kt)
├── routes/
│   ├── AuthRoutes.kt     # POST /auth/sync
│   └── MovieRoutes.kt    # GET+POST+PUT /movies
├── domain/
│   ├── model/
│   │   ├── Movie.kt
│   │   ├── Genre.kt      # enum class Genre
│   │   └── User.kt
│   ├── table/
│   │   ├── MoviesTable.kt
│   │   └── UsersTable.kt
│   └── service/
│       ├── MovieService.kt
│       └── UserService.kt
└── util/
    └── PrincipalExt.kt   # helpers to extract user/admin from ApplicationCall
```

---

## Key Dependencies to Add

In `gradle/libs.versions.toml`:
```toml
postgresql = "42.7.4"
exposed-jdbc = "1.2.0"   # replace exposed-r2dbc
```

In `build.gradle.kts`:
```kotlin
implementation(libs.postgresql)
implementation(libs.exposed.jdbc)   // replace exposed-r2dbc + h2 deps
```

Remove: `exposed-r2dbc`, `h2database-h2`, `h2database-r2dbc`

---

## Environment Variables

| Variable | Description |
|---|---|
| `DATABASE_URL` | Supabase PostgreSQL JDBC URL (`jdbc:postgresql://...`) |
| `DATABASE_USER` | DB user (Supabase: `postgres`) |
| `DATABASE_PASSWORD` | DB password |
| `SUPABASE_JWT_ISSUER` | `https://<ref>.supabase.co/auth/v1` |
| `SUPABASE_JWKS_URI` | `https://<ref>.supabase.co/auth/v1/keys` |

---

## Deployment

- **Containerize:** `./gradlew buildFatJar` → `Dockerfile` FROM `eclipse-temurin:21-jre`
- **Registry:** Google Artifact Registry
- **Deploy:** `gcloud run deploy movie-server --image ...`
- **CI:** GitHub Actions → build → push → deploy

---

## Decisions Log

| Decision | Choice | Reason |
|---|---|---|
| Auth provider | Supabase Auth | Same vendor as DB; handles OAuth flow + JWT issuance |
| Photo upload | Client → Supabase Storage directly | Keeps Ktor stateless; no binary traffic |
| Genre | Kotlin enum + PostgreSQL ENUM | Fixed set; type-safe filtering |
| DB access | Exposed DSL over JDBC | Idiomatic Kotlin; sync JDBC simpler than R2DBC for this use case |
| Rating scale | 0.0–10.0 NUMERIC(3,1) | Industry standard range |
| Admin grant | Manual via Supabase dashboard (`is_admin` flag) | Simple for MVP; no promotion endpoint needed yet |
| Hosting | Cloud Run | Scales to zero; no cluster ops; fits containerized Ktor |
