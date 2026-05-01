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

class FavoriteRoutesTest {

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
    fun `POST movies favorite without JWT returns 401`() = routeTest {
        val movieId = UUID.randomUUID()
        val response = client.post("/movies/$movieId/favorite")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `POST movies favorite with JWT adds favorite and returns 200`() = routeTest {
        val userId = UUID.randomUUID()
        val movieId = UUID.randomUUID()
        every { favoriteService.addFavorite(userId, movieId) } just Runs

        val response = client.post("/movies/$movieId/favorite") {
            header(HttpHeaders.Authorization, "Bearer ${createTestToken(userId)}")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        verify { favoriteService.addFavorite(userId, movieId) }
    }

    @Test
    fun `DELETE movies favorite without JWT returns 401`() = routeTest {
        val movieId = UUID.randomUUID()
        val response = client.delete("/movies/$movieId/favorite")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `DELETE movies favorite with JWT removes favorite and returns 200`() = routeTest {
        val userId = UUID.randomUUID()
        val movieId = UUID.randomUUID()
        every { favoriteService.removeFavorite(userId, movieId) } returns 1

        val response = client.delete("/movies/$movieId/favorite") {
            header(HttpHeaders.Authorization, "Bearer ${createTestToken(userId)}")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        verify { favoriteService.removeFavorite(userId, movieId) }
    }
}
