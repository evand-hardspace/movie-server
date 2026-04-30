package com.evandhardspace.movie.server.routes

import com.evandhardspace.movie.server.configureTestApp
import com.evandhardspace.movie.server.createTestToken
import com.evandhardspace.movie.server.domain.service.FavoriteService
import com.evandhardspace.movie.server.domain.service.MovieService
import com.evandhardspace.movie.server.domain.service.UserService
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.*
import java.util.UUID
import kotlin.test.*

class AuthRoutesTest {

    private val userService = mockk<UserService>()
    private val movieService = mockk<MovieService>()
    private val favoriteService = mockk<FavoriteService>()

    @AfterTest
    fun tearDown() = unmockkAll()

    private fun routeTest(block: suspend ApplicationTestBuilder.() -> Unit) = testApplication {
        configureTestApp(userService, movieService, favoriteService)
        block()
    }

    @Test
    fun `POST auth sync with valid JWT upserts user and returns 200`() = routeTest {
        val userId = UUID.randomUUID()
        every { userService.upsertUser(userId, any()) } just Runs

        val response = client.post("/auth/sync") {
            header(HttpHeaders.Authorization, "Bearer ${createTestToken(userId)}")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        verify { userService.upsertUser(userId, any()) }
    }

    @Test
    fun `POST auth sync without JWT returns 401`() = routeTest {
        val response = client.post("/auth/sync")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
}
