package com.evandhardspace.movie.server

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.evandhardspace.movie.server.domain.model.Genre
import com.evandhardspace.movie.server.domain.model.Movie
import com.evandhardspace.movie.server.domain.service.AuthService
import com.evandhardspace.movie.server.domain.service.FavoriteService
import com.evandhardspace.movie.server.domain.service.MovieService
import com.evandhardspace.movie.server.domain.service.UserService
import io.mockk.mockk
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.di.*
import io.ktor.server.response.*
import io.ktor.server.testing.*
import java.util.UUID
import kotlin.time.Instant

internal const val TEST_JWT_SECRET = "test-secret-key-for-unit-testing-only"

internal fun createTestToken(
    userId: UUID = UUID.randomUUID(),
    email: String = "test@example.com",
): String = JWT.create()
    .withSubject(userId.toString())
    .withClaim("email", email)
    .withAudience("authenticated")
    .sign(Algorithm.HMAC256(TEST_JWT_SECRET))

internal fun ApplicationTestBuilder.configureTestApp(
    userService: UserService,
    movieService: MovieService,
    favoriteService: FavoriteService,
    authService: AuthService = mockk(),
) {
    // install at ApplicationTestBuilder level to avoid ambiguity with Application.install
    install(ContentNegotiation) { json() }
    install(Authentication) {
        jwt("auth-jwt") {
            verifier(
                JWT.require(Algorithm.HMAC256(TEST_JWT_SECRET))
                    .withAudience("authenticated")
                    .build()
            )
            validate { credential ->
                if (credential.payload.audience.contains("authenticated")) JWTPrincipal(credential.payload) else null
            }
            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Unauthorized"))
            }
        }
    }
    application {
        dependencies {
            provide<AuthService> { authService }
            provide<UserService> { userService }
            provide<MovieService> { movieService }
            provide<FavoriteService> { favoriteService }
        }
        configureRouting()
    }
}

internal fun testMovie(id: UUID = UUID.randomUUID()) = Movie(
    id = id.toString(),
    title = "Test Movie",
    description = "A test description",
    genre = Genre.ACTION,
    rating = 7.5,
    photoUrl = null,
    createdAt = Instant.fromEpochMilliseconds(0),
)
