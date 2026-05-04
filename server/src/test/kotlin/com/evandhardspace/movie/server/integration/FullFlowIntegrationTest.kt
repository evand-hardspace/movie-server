package com.evandhardspace.movie.server.integration

import com.evandhardspace.movie.server.domain.service.AuthTokenResponse
import com.evandhardspace.movie.server.routes.UserResponse
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.builtins.ListSerializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FullFlowIntegrationTest {

    // region Auth

    @Test
    fun `register returns 201 with tokens`() = integrationTest {
        val response = client.register("user@example.com", "password123")

        assertEquals(HttpStatusCode.Created, response.status)
        val tokens = testJson.decodeFromString<AuthTokenResponse>(response.bodyAsText())
        assertTrue(tokens.accessToken.isNotBlank())
        assertTrue(tokens.refreshToken.isNotBlank())
    }

    @Test
    fun `duplicate registration returns 422`() = integrationTest {
        client.register("user@example.com", "password123")
        val response = client.register("user@example.com", "other")

        assertEquals(HttpStatusCode.UnprocessableEntity, response.status)
    }

    @Test
    fun `login with correct credentials returns 200 with tokens`() = integrationTest {
        client.register("user@example.com", "password123")
        val response = client.login("user@example.com", "password123")

        assertEquals(HttpStatusCode.OK, response.status)
        val tokens = testJson.decodeFromString<AuthTokenResponse>(response.bodyAsText())
        assertTrue(tokens.accessToken.isNotBlank())
    }

    @Test
    fun `login with wrong password returns 401`() = integrationTest {
        client.register("user@example.com", "password123")
        val response = client.login("user@example.com", "wrongpassword")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `login with unknown email returns 401`() = integrationTest {
        val response = client.login("nobody@example.com", "password123")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    // endregion

    // region Token refresh

    @Test
    fun `refresh with valid token returns 200 with new tokens`() = integrationTest {
        val tokens = client.registerAndGetTokens("user@example.com")
        val response = client.post("/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody("""{"refresh_token":"${tokens.refreshToken}"}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val newTokens = testJson.decodeFromString<AuthTokenResponse>(response.bodyAsText())
        assertTrue(newTokens.accessToken.isNotBlank())
        assertTrue(newTokens.refreshToken != tokens.refreshToken)
    }

    @Test
    fun `refresh token is single-use - second use returns 401`() = integrationTest {
        val tokens = client.registerAndGetTokens("user@example.com")
        client.post("/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody("""{"refresh_token":"${tokens.refreshToken}"}""")
        }
        val secondResponse = client.post("/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody("""{"refresh_token":"${tokens.refreshToken}"}""")
        }

        assertEquals(HttpStatusCode.Unauthorized, secondResponse.status)
    }

    @Test
    fun `refresh with invalid token returns 401`() = integrationTest {
        val response = client.post("/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody("""{"refresh_token":"not-a-real-token"}""")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    // endregion

    // region Users/me

    @Test
    fun `GET users-me without auth returns 401`() = integrationTest {
        val response = client.get("/users/me")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `GET users-me with valid token returns current user`() = integrationTest {
        val tokens = client.registerAndGetTokens("user@example.com")
        val response = client.get("/users/me") {
            header(HttpHeaders.Authorization, bearerHeader(tokens.accessToken))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val user = testJson.decodeFromString<UserResponse>(response.bodyAsText())
        assertEquals("user@example.com", user.email)
        assertNotNull(user.id)
    }

    // endregion

    // region Movie CRUD

    @Test
    fun `GET movies is public and returns empty list initially`() = integrationTest {
        val response = client.get("/movies")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("[]", response.bodyAsText().trim())
    }

    @Test
    fun `POST movie without auth returns 401`() = integrationTest {
        val response = client.post("/movies") {
            contentType(ContentType.Application.Json)
            setBody("""{"title":"Inception","genre":"THRILLER"}""")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `POST movie as regular user returns 403`() = integrationTest {
        val tokens = client.registerAndGetTokens("user@example.com")
        val response = client.post("/movies") {
            header(HttpHeaders.Authorization, bearerHeader(tokens.accessToken))
            contentType(ContentType.Application.Json)
            setBody("""{"title":"Inception","genre":"THRILLER"}""")
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun `full movie CRUD flow as admin`() = integrationTest {
        val tokens = client.registerAndGetTokens("admin@example.com")
        val adminId = extractUserId(tokens.accessToken)
        promoteToAdmin(adminId)

        // Re-login to get a fresh token that will still be valid (same token works, role check is DB-based)
        val freshTokens = testJson.decodeFromString<AuthTokenResponse>(
            client.login("admin@example.com", "password123").bodyAsText()
        )
        val auth = bearerHeader(freshTokens.accessToken)

        // Create
        val createResponse = client.post("/movies") {
            header(HttpHeaders.Authorization, auth)
            contentType(ContentType.Application.Json)
            setBody("""{"title":"Inception","genre":"THRILLER","rating":8.8}""")
        }
        assertEquals(HttpStatusCode.Created, createResponse.status)
        val created = testJson.decodeFromString<MoviePayload>(createResponse.bodyAsText())
        assertEquals("Inception", created.title)
        val movieId = created.id

        // Read list
        val listResponse = client.get("/movies")
        assertEquals(HttpStatusCode.OK, listResponse.status)
        assertTrue(listResponse.bodyAsText().contains("Inception"))

        // Read by id
        val getResponse = client.get("/movies/$movieId")
        assertEquals(HttpStatusCode.OK, getResponse.status)
        assertEquals("Inception", testJson.decodeFromString<MoviePayload>(getResponse.bodyAsText()).title)

        // Update
        val updateResponse = client.put("/movies/$movieId") {
            header(HttpHeaders.Authorization, auth)
            contentType(ContentType.Application.Json)
            setBody("""{"title":"Inception Updated","genre":"THRILLER"}""")
        }
        assertEquals(HttpStatusCode.OK, updateResponse.status)
        assertEquals("Inception Updated", testJson.decodeFromString<MoviePayload>(updateResponse.bodyAsText()).title)

        // Delete
        val deleteResponse = client.delete("/movies/$movieId") {
            header(HttpHeaders.Authorization, auth)
        }
        assertEquals(HttpStatusCode.NoContent, deleteResponse.status)

        // Confirm deleted
        val afterDeleteResponse = client.get("/movies/$movieId")
        assertEquals(HttpStatusCode.NotFound, afterDeleteResponse.status)
    }

    @Test
    fun `GET movies with genre filter returns only matching movies`() = integrationTest {
        val tokens = client.registerAndGetTokens("admin@example.com")
        promoteToAdmin(extractUserId(tokens.accessToken))
        val fresh = testJson.decodeFromString<AuthTokenResponse>(
            client.login("admin@example.com", "password123").bodyAsText()
        )
        val auth = bearerHeader(fresh.accessToken)

        client.post("/movies") {
            header(HttpHeaders.Authorization, auth)
            contentType(ContentType.Application.Json)
            setBody("""{"title":"Action Movie","genre":"ACTION"}""")
        }
        client.post("/movies") {
            header(HttpHeaders.Authorization, auth)
            contentType(ContentType.Application.Json)
            setBody("""{"title":"Drama Movie","genre":"DRAMA"}""")
        }

        val response = client.get("/movies?genre=ACTION")
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("Action Movie"))
        assertFalse(body.contains("Drama Movie"))
    }

    @Test
    fun `GET movies with invalid genre returns 400`() = integrationTest {
        val response = client.get("/movies?genre=INVALID")

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `GET movie by invalid UUID returns 400`() = integrationTest {
        val response = client.get("/movies/not-a-uuid")

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    // endregion

    // region Paginated movies

    @Test
    fun `GET movies with page param on empty db returns empty PagedMoviesResponse`() = integrationTest {
        val response = client.get("/movies?page=1")

        assertEquals(HttpStatusCode.OK, response.status)
        val paged = testJson.decodeFromString<PagedMoviesPayload>(response.bodyAsText())
        assertEquals(1, paged.page)
        assertEquals(20, paged.pageSize)
        assertEquals(0L, paged.total)
        assertEquals(1, paged.totalPages)
        assertTrue(paged.items.isEmpty())
    }

    @Test
    fun `GET movies paginated returns correct totals and page`() = integrationTest {
        val auth = bearerHeader(adminAuth())

        repeat(3) { i ->
            client.post("/movies") {
                header(HttpHeaders.Authorization, auth)
                contentType(ContentType.Application.Json)
                setBody("""{"title":"Movie $i","genre":"ACTION"}""")
            }
        }

        val response = client.get("/movies?page=1&page_size=2")

        assertEquals(HttpStatusCode.OK, response.status)
        val paged = testJson.decodeFromString<PagedMoviesPayload>(response.bodyAsText())
        assertEquals(1, paged.page)
        assertEquals(2, paged.pageSize)
        assertEquals(3L, paged.total)
        assertEquals(2, paged.totalPages)
        assertEquals(2, paged.items.size)
    }

    @Test
    fun `GET movies page 2 returns remaining items`() = integrationTest {
        val auth = bearerHeader(adminAuth())

        repeat(3) { i ->
            client.post("/movies") {
                header(HttpHeaders.Authorization, auth)
                contentType(ContentType.Application.Json)
                setBody("""{"title":"Movie $i","genre":"ACTION"}""")
            }
        }

        val response = client.get("/movies?page=2&page_size=2")

        assertEquals(HttpStatusCode.OK, response.status)
        val paged = testJson.decodeFromString<PagedMoviesPayload>(response.bodyAsText())
        assertEquals(2, paged.page)
        assertEquals(1, paged.items.size)
    }

    @Test
    fun `GET movies paginated with genre filter returns only matching movies`() = integrationTest {
        val auth = bearerHeader(adminAuth())

        client.post("/movies") {
            header(HttpHeaders.Authorization, auth)
            contentType(ContentType.Application.Json)
            setBody("""{"title":"Action Movie","genre":"ACTION"}""")
        }
        client.post("/movies") {
            header(HttpHeaders.Authorization, auth)
            contentType(ContentType.Application.Json)
            setBody("""{"title":"Drama Movie","genre":"DRAMA"}""")
        }

        val response = client.get("/movies?page=1&genre=ACTION")

        assertEquals(HttpStatusCode.OK, response.status)
        val paged = testJson.decodeFromString<PagedMoviesPayload>(response.bodyAsText())
        assertEquals(1L, paged.total)
        assertEquals(1, paged.items.size)
        assertTrue(paged.items.first().title.contains("Action"))
    }

    @Test
    fun `GET movies paginated enriches is_favorited when authenticated`() = integrationTest {
        val auth = bearerHeader(adminAuth())
        val created = testJson.decodeFromString<MoviePayload>(
            client.post("/movies") {
                header(HttpHeaders.Authorization, auth)
                contentType(ContentType.Application.Json)
                setBody("""{"title":"Dune","genre":"SCI_FI"}""")
            }.bodyAsText()
        )

        val userTokens = client.registerAndGetTokens("user@example.com")
        val userAuth = bearerHeader(userTokens.accessToken)

        client.post("/movies/${created.id}/favorite") {
            header(HttpHeaders.Authorization, userAuth)
        }

        val response = client.get("/movies?page=1") {
            header(HttpHeaders.Authorization, userAuth)
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("\"is_favorited\":true"))
    }

    // endregion

    // region Favorites

    @Test
    fun `full favorites flow`() = integrationTest {
        // Setup admin + movie
        val adminTokens = client.registerAndGetTokens("admin@example.com")
        promoteToAdmin(extractUserId(adminTokens.accessToken))
        val freshAdmin = testJson.decodeFromString<AuthTokenResponse>(
            client.login("admin@example.com", "password123").bodyAsText()
        )
        val created = testJson.decodeFromString<MoviePayload>(
            client.post("/movies") {
                header(HttpHeaders.Authorization, bearerHeader(freshAdmin.accessToken))
                contentType(ContentType.Application.Json)
                setBody("""{"title":"Interstellar","genre":"SCI_FI"}""")
            }.bodyAsText()
        )
        val movieId = created.id

        // Regular user
        val userTokens = client.registerAndGetTokens("user@example.com")
        val userAuth = bearerHeader(userTokens.accessToken)

        // Not favorited initially
        val before = client.get("/movies/$movieId") {
            header(HttpHeaders.Authorization, userAuth)
        }
        assertFalse(before.bodyAsText().contains("\"is_favorited\":true"))

        // Add favorite
        val addResponse = client.post("/movies/$movieId/favorite") {
            header(HttpHeaders.Authorization, userAuth)
        }
        assertEquals(HttpStatusCode.OK, addResponse.status)

        // Now favorited
        val after = client.get("/movies/$movieId") {
            header(HttpHeaders.Authorization, userAuth)
        }
        assertTrue(after.bodyAsText().contains("\"is_favorited\":true"))

        // Also reflected in list
        val list = client.get("/movies") {
            header(HttpHeaders.Authorization, userAuth)
        }
        assertTrue(list.bodyAsText().contains("\"is_favorited\":true"))

        // Remove favorite
        val removeResponse = client.delete("/movies/$movieId/favorite") {
            header(HttpHeaders.Authorization, userAuth)
        }
        assertEquals(HttpStatusCode.OK, removeResponse.status)

        // No longer favorited
        val afterRemove = client.get("/movies/$movieId") {
            header(HttpHeaders.Authorization, userAuth)
        }
        assertFalse(afterRemove.bodyAsText().contains("\"is_favorited\":true"))
    }

    @Test
    fun `POST favorite without auth returns 401`() = integrationTest {
        val response = client.post("/movies/00000000-0000-0000-0000-000000000001/favorite")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    // endregion

    // region User management

    @Test
    fun `GET users without auth returns 401`() = integrationTest {
        val response = client.get("/users")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `GET users as regular user returns 403`() = integrationTest {
        val tokens = client.registerAndGetTokens("user@example.com")
        val response = client.get("/users") {
            header(HttpHeaders.Authorization, bearerHeader(tokens.accessToken))
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun `full user management flow as super_admin`() = integrationTest {
        val superTokens = client.registerAndGetTokens("super@example.com")
        promoteToSuperAdmin(extractUserId(superTokens.accessToken))
        val freshSuper = testJson.decodeFromString<AuthTokenResponse>(
            client.login("super@example.com", "password123").bodyAsText()
        )
        val superAuth = bearerHeader(freshSuper.accessToken)
        val superId = extractUserId(freshSuper.accessToken)

        // Register a second user
        val userTokens = client.registerAndGetTokens("regular@example.com")
        val regularId = extractUserId(userTokens.accessToken)

        // List all users
        val listResponse = client.get("/users") {
            header(HttpHeaders.Authorization, superAuth)
        }
        assertEquals(HttpStatusCode.OK, listResponse.status)
        val users = testJson.decodeFromString(ListSerializer(UserResponse.serializer()), listResponse.bodyAsText())
        assertEquals(2, users.size)

        // Promote regular user to admin
        val updateResponse = client.put("/users/$regularId/role") {
            header(HttpHeaders.Authorization, superAuth)
            contentType(ContentType.Application.Json)
            setBody("""{"role":"admin"}""")
        }
        assertEquals(HttpStatusCode.OK, updateResponse.status)

        // Cannot change own role
        val selfUpdateResponse = client.put("/users/$superId/role") {
            header(HttpHeaders.Authorization, superAuth)
            contentType(ContentType.Application.Json)
            setBody("""{"role":"user"}""")
        }
        assertEquals(HttpStatusCode.Forbidden, selfUpdateResponse.status)
    }

    // endregion
}

@kotlinx.serialization.Serializable
private data class MoviePayload(
    val id: String,
    val title: String,
)

@kotlinx.serialization.Serializable
private data class PagedMoviesPayload(
    val items: List<MoviePayload>,
    val page: Int,
    @SerialName("page_size") val pageSize: Int,
    val total: Long,
    @SerialName("total_pages") val totalPages: Int,
)
