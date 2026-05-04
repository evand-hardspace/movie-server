package com.evandhardspace.movie.server.routes

import com.evandhardspace.movie.server.configureTestApp
import com.evandhardspace.movie.server.domain.service.AuthException
import com.evandhardspace.movie.server.domain.service.AuthResult
import com.evandhardspace.movie.server.domain.service.AuthService
import com.evandhardspace.movie.server.domain.service.AuthTokenResponse
import com.evandhardspace.movie.server.domain.service.FavoriteService
import com.evandhardspace.movie.server.domain.service.MovieService
import com.evandhardspace.movie.server.domain.service.UserService
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.*
import java.util.UUID
import kotlin.test.*

class AuthRoutesTest {

    private val authService = mockk<AuthService>()
    private val userService = mockk<UserService>()
    private val movieService = mockk<MovieService>()
    private val favoriteService = mockk<FavoriteService>()

    @AfterTest
    fun tearDown() = unmockkAll()

    private fun routeTest(block: suspend ApplicationTestBuilder.() -> Unit) = testApplication {
        configureTestApp(userService, movieService, favoriteService, authService)
        block()
    }

    private fun testTokenResponse() = AuthTokenResponse(
        accessToken = "access.token.value",
        refreshToken = "refresh-token-uuid",
        tokenType = "Bearer",
        expiresIn = 3600,
    )

    @Test
    fun `POST auth register returns 201 with tokens on success`() = routeTest {
        val userId = UUID.randomUUID()
        every { authService.signUp(any(), any()) } returns AuthResult(testTokenResponse(), userId, "user@example.com")

        val response = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"user@example.com","password":"secret123"}""")
        }

        assertEquals(HttpStatusCode.Created, response.status)
        assertTrue(response.bodyAsText().contains("access_token"))
    }

    @Test
    fun `POST auth register returns 422 when email already registered`() = routeTest {
        every { authService.signUp(any(), any()) } throws AuthException("Email already registered", HttpStatusCode.UnprocessableEntity)

        val response = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"existing@example.com","password":"secret123"}""")
        }

        assertEquals(HttpStatusCode.UnprocessableEntity, response.status)
    }

    @Test
    fun `POST auth login returns 200 with tokens on success`() = routeTest {
        val userId = UUID.randomUUID()
        every { authService.signIn(any(), any()) } returns AuthResult(testTokenResponse(), userId, "user@example.com")

        val response = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"user@example.com","password":"secret123"}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("access_token"))
    }

    @Test
    fun `POST auth login returns 401 on wrong credentials`() = routeTest {
        every { authService.signIn(any(), any()) } throws AuthException("Invalid email or password", HttpStatusCode.Unauthorized)

        val response = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"user@example.com","password":"wrong"}""")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `POST auth refresh returns 200 with new tokens on valid refresh token`() = routeTest {
        every { authService.refresh(any()) } returns testTokenResponse()

        val response = client.post("/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody("""{"refresh_token":"valid-refresh-uuid"}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("access_token"))
    }

    @Test
    fun `POST auth refresh returns 401 on invalid or expired refresh token`() = routeTest {
        every { authService.refresh(any()) } throws AuthException("Refresh token invalid or expired", HttpStatusCode.Unauthorized)

        val response = client.post("/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody("""{"refresh_token":"bad-token"}""")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
}
