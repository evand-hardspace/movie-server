package com.evandhardspace.movie.server.integration

import com.auth0.jwt.JWT
import com.evandhardspace.movie.server.TEST_JWT_SECRET
import com.evandhardspace.movie.server.configureRouting
import com.evandhardspace.movie.server.domain.model.UserRole
import com.evandhardspace.movie.server.domain.service.AuthTokenResponse
import com.evandhardspace.movie.server.domain.service.UserService
import com.evandhardspace.movie.server.plugins.configureDatabase
import com.evandhardspace.movie.server.plugins.configureDependencyInjection
import com.evandhardspace.movie.server.plugins.configureSecurity
import com.evandhardspace.movie.server.plugins.configureSerialization
import io.ktor.client.HttpClient
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.Application
import io.ktor.server.config.*
import io.ktor.server.plugins.di.dependencies
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.util.UUID

internal const val INTEGRATION_JWT_SECRET = "integration-test-jwt-secret-32bytes!"

internal val testJson = Json { ignoreUnknownKeys = true }

// In-memory SQLite does not work reliably with HikariCP's connection pool because each
// connection gets its own isolated database. A temp file gives identical semantics with
// full cross-connection isolation per test.
internal fun integrationTest(block: suspend ApplicationTestBuilder.() -> Unit) {
    val tmpFile = Files.createTempFile("testdb_", ".db")
    try {
        testApplication {
            environment {
                config = MapApplicationConfig(
                    "jwt.secret" to INTEGRATION_JWT_SECRET,
                    "jwt.audience" to "authenticated",
                )
            }
            application {
                configureDependencyInjection()
                configureTestVariables()
                configureDatabase("jdbc:sqlite:${tmpFile.toAbsolutePath()}")
                configureSerialization()
                configureSecurity()
                configureRouting()
            }
            block()
        }
    } finally {
        Files.deleteIfExists(tmpFile)
    }
}

private fun Application.configureTestVariables() {
    dependencies {
        provide("jwt.secret") { TEST_JWT_SECRET }
        provide("jwt.audience") { "authenticated" }
    }
}

internal fun promoteToAdmin(userId: UUID) {
    UserService().updateRole(userId, UserRole.ADMIN)
}

internal fun promoteToSuperAdmin(userId: UUID) {
    UserService().updateRole(userId, UserRole.SUPER_ADMIN)
}

internal fun extractUserId(accessToken: String): UUID =
    UUID.fromString(JWT.decode(accessToken).subject)

internal suspend fun HttpClient.register(email: String, password: String): HttpResponse =
    post("/auth/register") {
        contentType(ContentType.Application.Json)
        setBody("""{"email":"$email","password":"$password"}""")
    }

internal suspend fun HttpClient.login(email: String, password: String): HttpResponse =
    post("/auth/login") {
        contentType(ContentType.Application.Json)
        setBody("""{"email":"$email","password":"$password"}""")
    }

internal suspend fun HttpClient.registerAndGetTokens(
    email: String,
    password: String = "password123",
): AuthTokenResponse {
    val body = register(email, password).bodyAsText()
    return testJson.decodeFromString(body)
}

internal fun bearerHeader(token: String) = "Bearer $token"
