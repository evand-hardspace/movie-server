package com.evandhardspace.movie.server.routes

import com.evandhardspace.movie.server.configureTestApp
import com.evandhardspace.movie.server.createTestToken
import com.evandhardspace.movie.server.domain.model.User
import com.evandhardspace.movie.server.domain.model.UserRole
import com.evandhardspace.movie.server.domain.service.FavoriteService
import com.evandhardspace.movie.server.domain.service.MovieService
import com.evandhardspace.movie.server.domain.service.UserService
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class UserRoutesTest {

    private val userService = mockk<UserService>()
    private val movieService = mockk<MovieService>()
    private val favoriteService = mockk<FavoriteService>()

    private val userId = UUID.randomUUID()
    private val targetId = UUID.randomUUID()
    private val token = createTestToken(userId)

    @AfterTest
    fun tearDown() = unmockkAll()

    private fun routeTest(block: suspend ApplicationTestBuilder.(HttpClient) -> Unit) = testApplication {
        configureTestApp(userService, movieService, favoriteService)
        block(createClient {})
    }

    @Test
    fun `GET users me without JWT returns 401`() = routeTest { client ->
        val response = client.get("/users/me")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `GET users me with JWT returns current user`() = routeTest { client ->
        every { userService.getUser(userId) } returns User(userId, "test@example.com", UserRole.USER)
        val response = client.get("/users/me") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `GET users me when user not found returns 404`() = routeTest { client ->
        every { userService.getUser(userId) } returns null
        val response = client.get("/users/me") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `GET users without JWT returns 401`() = routeTest { client ->
        val response = client.get("/users")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `GET users with non-super-admin returns 403`() = routeTest { client ->
        every { userService.isSuperAdmin(userId) } returns false
        val response = client.get("/users") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun `GET users with super admin returns list`() = routeTest { client ->
        every { userService.isSuperAdmin(userId) } returns true
        every { userService.getAllUsers() } returns listOf(
            User(userId, "test@example.com", UserRole.SUPER_ADMIN),
            User(targetId, "other@example.com", UserRole.USER),
        )
        val response = client.get("/users") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `PUT users role without JWT returns 401`() = routeTest { client ->
        val response = client.put("/users/$targetId/role") {
            contentType(ContentType.Application.Json)
            setBody("""{"role":"admin"}""")
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `PUT users role with non-super-admin returns 403`() = routeTest { client ->
        every { userService.isSuperAdmin(userId) } returns false
        val response = client.put("/users/$targetId/role") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{"role":"admin"}""")
        }
        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun `PUT users role with super admin promotes user and returns 200`() = routeTest { client ->
        every { userService.isSuperAdmin(userId) } returns true
        every { userService.updateRole(targetId, UserRole.ADMIN) } returns true
        val response = client.put("/users/$targetId/role") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{"role":"admin"}""")
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `PUT users role with unknown target user returns 404`() = routeTest { client ->
        every { userService.isSuperAdmin(userId) } returns true
        every { userService.updateRole(targetId, UserRole.ADMIN) } returns false
        val response = client.put("/users/$targetId/role") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{"role":"admin"}""")
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}
