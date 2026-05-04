# Movie Server — Backlog

Status legend: `[ ]` pending · `[~]` in progress · `[x]` done · `[!]` blocked

---

## Migration: Supabase → SQLite + Self-Owned JWT

| # | Status | Task |
|---|--------|------|
| M1 | `[ ]` | `libs.versions.toml` — remove `postgresql`, add `sqlite-jdbc = "3.49.1.0"` + `bcrypt = "0.10.2"` |
| M2 | `[ ]` | `server/build.gradle.kts` — remove postgresql + ktor-client-* deps, add sqlite-jdbc + bcrypt |
| M3 | `[ ]` | `domain/table/UsersTable.kt` — add `password_hash` column, replace PGobject enum → `enumeration<UserRole>`, replace `timestampWithTimeZone` → `long` |
| M4 | `[ ]` | Create `domain/table/RefreshTokensTable.kt` — columns: `token` (PK), `userId` (FK), `expiresAt` (long), `isRevoked` (bool) |
| M5 | `[ ]` | `domain/table/MoviesTable.kt` — replace PGobject enum → `enumeration<Genre>`, replace `timestampWithTimeZone` → `long` |
| M6 | `[ ]` | `domain/table/FavoritesTable.kt` — replace `timestampWithTimeZone` → `long` |
| M7 | `[ ]` | `plugins/Database.kt` — switch to SQLite JDBC (`SQLITE_FILE` env var), call `SchemaUtils.create(...)` on startup |
| M8 | `[ ]` | `domain/service/UserService.kt` — add `createUser(email, passwordHash)` + `findByEmail(email)`, update timestamp writes to epoch ms |
| M9 | `[ ]` | `domain/service/AuthService.kt` — full rewrite: BCrypt hashing, HMAC256 JWT issuance, refresh tokens stored in DB |
| M10 | `[ ]` | `plugins/Security.kt` — replace JwkProviderBuilder/JWKS with HMAC256 verifier using `JWT_SECRET` |
| M11 | `[ ]` | `plugins/DI.kt` — remove Supabase config reads, wire `AuthService(jwtSecret, userService)` |
| M12 | `[ ]` | `application.yaml` — replace `jwt.jwksUri`/`supabase.*` with `jwt.secret` |
| M13 | `[ ]` | `routes/AuthRoutes.kt` — remove `/auth/sync` route, remove manual `userService.upsertUser` calls |
| M14 | `[ ]` | `domain/service/MovieService.kt` + `FavoriteService.kt` — replace `OffsetDateTime` with epoch ms longs |
| M15 | `[ ]` | `test/.../AuthRoutesTest.kt` — remove `/auth/sync` tests, add register/login/refresh tests |
| M16 | `[ ]` | `plugins/` — add `configureSeed()` plugin: seeds super_admin from `SEED_ADMIN_EMAIL`/`SEED_ADMIN_PASSWORD` env vars if no users exist |
| M17 | `[ ]` | `.github/workflows/deploy.yml` — remove Supabase/DB secrets, add `JWT_SECRET` + `SQLITE_FILE` |
| M18 | `[ ]` | `CLAUDE.md` — update arch, env vars, auth flow, tech stack, decisions log |
