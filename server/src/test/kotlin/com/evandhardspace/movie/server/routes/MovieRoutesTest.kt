package com.evandhardspace.movie.server.routes

import com.evandhardspace.movie.server.configureTestApp
import com.evandhardspace.movie.server.createTestToken
import com.evandhardspace.movie.server.domain.model.Genre
import com.evandhardspace.movie.server.domain.service.FavoriteService
import com.evandhardspace.movie.server.domain.service.MovieService
import com.evandhardspace.movie.server.domain.service.PagedMovies
import com.evandhardspace.movie.server.domain.service.UserService
import com.evandhardspace.movie.server.testMovie
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.*
import java.util.UUID
import kotlin.test.*

class MovieRoutesTest {

    private val userService = mockk<UserService>()
    private val movieService = mockk<MovieService>()
    private val favoriteService = mockk<FavoriteService>()

    @AfterTest
    fun tearDown() = unmockkAll()

    private fun routeTest(block: suspend ApplicationTestBuilder.() -> Unit) = testApplication {
        configureTestApp(userService, movieService, favoriteService)
        block()
    }

    // region GET /movies

    @Test
    fun `GET movies without auth returns 200 with list`() = routeTest {
        every { movieService.getMovies(null) } returns listOf(testMovie())

        val response = client.get("/movies")

        assertEquals(HttpStatusCode.OK, response.status)
        verify { movieService.getMovies(null) }
    }

    @Test
    fun `GET movies with genre filter returns 200`() = routeTest {
        every { movieService.getMovies(Genre.ACTION) } returns listOf(testMovie())

        val response = client.get("/movies?genre=ACTION")

        assertEquals(HttpStatusCode.OK, response.status)
        verify { movieService.getMovies(Genre.ACTION) }
    }

    @Test
    fun `GET movies with invalid genre returns 400`() = routeTest {
        val response = client.get("/movies?genre=INVALID_GENRE")
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `GET movies with JWT enriches is_favorited`() = routeTest {
        val userId = UUID.randomUUID()
        val movieId = UUID.randomUUID()
        every { movieService.getMovies(null) } returns listOf(testMovie(movieId))
        every { favoriteService.getFavoritedMovieIds(userId) } returns setOf(movieId)

        val response = client.get("/movies") {
            header(HttpHeaders.Authorization, "Bearer ${createTestToken(userId)}")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("\"is_favorited\":true"))
    }

    // endregion

    // region GET /movies/{id}

    @Test
    fun `GET movies by id without auth returns 200`() = routeTest {
        val movieId = UUID.randomUUID()
        every { movieService.getMovieById(movieId) } returns testMovie(movieId)

        val response = client.get("/movies/$movieId")

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `GET movies by id with JWT enriches is_favorited`() = routeTest {
        val userId = UUID.randomUUID()
        val movieId = UUID.randomUUID()
        every { movieService.getMovieById(movieId) } returns testMovie(movieId)
        every { favoriteService.isFavorited(userId, movieId) } returns true

        val response = client.get("/movies/$movieId") {
            header(HttpHeaders.Authorization, "Bearer ${createTestToken(userId)}")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("\"is_favorited\":true"))
    }

    @Test
    fun `GET movies by invalid UUID returns 400`() = routeTest {
        val response = client.get("/movies/not-a-uuid")
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `GET movies by id for missing movie returns 404`() = routeTest {
        val movieId = UUID.randomUUID()
        every { movieService.getMovieById(movieId) } returns null

        val response = client.get("/movies/$movieId")

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    // endregion

    // region POST /movies

    @Test
    fun `POST movies without JWT returns 401`() = routeTest {
        val response = client.post("/movies") {
            contentType(ContentType.Application.Json)
            setBody("""{"title":"Test","genre":"ACTION"}""")
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `POST movies with non-admin user returns 403`() = routeTest {
        val userId = UUID.randomUUID()
        every { userService.isAdmin(userId) } returns false

        val response = client.post("/movies") {
            header(HttpHeaders.Authorization, "Bearer ${createTestToken(userId)}")
            contentType(ContentType.Application.Json)
            setBody("""{"title":"Test","genre":"ACTION"}""")
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun `POST movies with admin user returns 201`() = routeTest {
        val userId = UUID.randomUUID()
        every { userService.isAdmin(userId) } returns true
        every { movieService.createMovie(any(), any(), any(), any(), any(), eq(userId)) } returns testMovie()

        val response = client.post("/movies") {
            header(HttpHeaders.Authorization, "Bearer ${createTestToken(userId)}")
            contentType(ContentType.Application.Json)
            setBody("""{"title":"Test","genre":"ACTION"}""")
        }

        assertEquals(HttpStatusCode.Created, response.status)
    }

    // endregion

    // region DELETE /movies/{id}

    @Test
    fun `DELETE movies without JWT returns 401`() = routeTest {
        val response = client.delete("/movies/${UUID.randomUUID()}")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `DELETE movies with non-admin user returns 403`() = routeTest {
        val userId = UUID.randomUUID()
        val movieId = UUID.randomUUID()
        every { userService.isAdmin(userId) } returns false

        val response = client.delete("/movies/$movieId") {
            header(HttpHeaders.Authorization, "Bearer ${createTestToken(userId)}")
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun `DELETE movies with admin returns 204`() = routeTest {
        val userId = UUID.randomUUID()
        val movieId = UUID.randomUUID()
        every { userService.isAdmin(userId) } returns true
        every { movieService.deleteMovie(movieId) } returns true

        val response = client.delete("/movies/$movieId") {
            header(HttpHeaders.Authorization, "Bearer ${createTestToken(userId)}")
        }

        assertEquals(HttpStatusCode.NoContent, response.status)
    }

    @Test
    fun `DELETE movies with admin for missing movie returns 404`() = routeTest {
        val userId = UUID.randomUUID()
        val movieId = UUID.randomUUID()
        every { userService.isAdmin(userId) } returns true
        every { movieService.deleteMovie(movieId) } returns false

        val response = client.delete("/movies/$movieId") {
            header(HttpHeaders.Authorization, "Bearer ${createTestToken(userId)}")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    // endregion

    // region PUT /movies/{id}

    @Test
    fun `PUT movies without JWT returns 401`() = routeTest {
        val movieId = UUID.randomUUID()
        val response = client.put("/movies/$movieId") {
            contentType(ContentType.Application.Json)
            setBody("""{"title":"Updated","genre":"DRAMA"}""")
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `PUT movies with non-admin user returns 403`() = routeTest {
        val userId = UUID.randomUUID()
        val movieId = UUID.randomUUID()
        every { userService.isAdmin(userId) } returns false

        val response = client.put("/movies/$movieId") {
            header(HttpHeaders.Authorization, "Bearer ${createTestToken(userId)}")
            contentType(ContentType.Application.Json)
            setBody("""{"title":"Updated","genre":"DRAMA"}""")
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun `PUT movies with admin user returns 200`() = routeTest {
        val userId = UUID.randomUUID()
        val movieId = UUID.randomUUID()
        every { userService.isAdmin(userId) } returns true
        every { movieService.updateMovie(eq(movieId), any(), any(), any(), any(), any()) } returns testMovie(movieId)

        val response = client.put("/movies/$movieId") {
            header(HttpHeaders.Authorization, "Bearer ${createTestToken(userId)}")
            contentType(ContentType.Application.Json)
            setBody("""{"title":"Updated","genre":"DRAMA"}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `PUT movies with admin user for missing movie returns 404`() = routeTest {
        val userId = UUID.randomUUID()
        val movieId = UUID.randomUUID()
        every { userService.isAdmin(userId) } returns true
        every { movieService.updateMovie(eq(movieId), any(), any(), any(), any(), any()) } returns null

        val response = client.put("/movies/$movieId") {
            header(HttpHeaders.Authorization, "Bearer ${createTestToken(userId)}")
            contentType(ContentType.Application.Json)
            setBody("""{"title":"Updated","genre":"DRAMA"}""")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    // endregion

    // region GET /movies paginated

    private fun pagedResult(vararg movies: java.util.UUID, page: Int = 1, pageSize: Int = 20) = PagedMovies(
        items = movies.map { testMovie(it) },
        page = page,
        pageSize = pageSize,
        total = movies.size.toLong(),
        totalPages = 1,
    )

    @Test
    fun `GET movies with page param returns PagedMoviesResponse`() = routeTest {
        val movieId = UUID.randomUUID()
        every { movieService.getMoviesPaged(null, 1, 20) } returns pagedResult(movieId)

        val response = client.get("/movies?page=1")

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("\"page\":1"))
        assertTrue(body.contains("\"total\":1"))
        assertTrue(body.contains("\"total_pages\":1"))
        verify { movieService.getMoviesPaged(null, 1, 20) }
    }

    @Test
    fun `GET movies paginated with genre filter passes genre to service`() = routeTest {
        every { movieService.getMoviesPaged(Genre.ACTION, 1, 20) } returns pagedResult(UUID.randomUUID())

        val response = client.get("/movies?page=1&genre=ACTION")

        assertEquals(HttpStatusCode.OK, response.status)
        verify { movieService.getMoviesPaged(Genre.ACTION, 1, 20) }
    }

    @Test
    fun `GET movies paginated respects page_size param`() = routeTest {
        every { movieService.getMoviesPaged(null, 1, 5) } returns pagedResult(UUID.randomUUID(), pageSize = 5)

        val response = client.get("/movies?page=1&page_size=5")

        assertEquals(HttpStatusCode.OK, response.status)
        verify { movieService.getMoviesPaged(null, 1, 5) }
    }

    @Test
    fun `GET movies paginated with JWT enriches is_favorited`() = routeTest {
        val userId = UUID.randomUUID()
        val movieId = UUID.randomUUID()
        every { movieService.getMoviesPaged(null, 1, 20) } returns pagedResult(movieId)
        every { favoriteService.getFavoritedMovieIds(userId) } returns setOf(movieId)

        val response = client.get("/movies?page=1") {
            header(HttpHeaders.Authorization, "Bearer ${createTestToken(userId)}")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("\"is_favorited\":true"))
    }

    // endregion

    // region GET /movies only_favorites filter

    @Test
    fun `GET movies with only_favorites without JWT returns 401`() = routeTest {
        val response = client.get("/movies?only_favorites=true")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `GET movies with only_favorites returns favorited movies with is_favorited true`() = routeTest {
        val userId = UUID.randomUUID()
        val movieId = UUID.randomUUID()
        val favIds = setOf(movieId)
        every { favoriteService.getFavoritedMovieIds(userId) } returns favIds
        every { movieService.getMovies(null, favIds) } returns listOf(testMovie(movieId))

        val response = client.get("/movies?only_favorites=true") {
            header(HttpHeaders.Authorization, "Bearer ${createTestToken(userId)}")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("\"is_favorited\":true"))
        verify { movieService.getMovies(null, favIds) }
    }

    @Test
    fun `GET movies with only_favorites and genre passes both filters to service`() = routeTest {
        val userId = UUID.randomUUID()
        val movieId = UUID.randomUUID()
        val favIds = setOf(movieId)
        every { favoriteService.getFavoritedMovieIds(userId) } returns favIds
        every { movieService.getMovies(Genre.ACTION, favIds) } returns listOf(testMovie(movieId))

        val response = client.get("/movies?only_favorites=true&genre=ACTION") {
            header(HttpHeaders.Authorization, "Bearer ${createTestToken(userId)}")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        verify { movieService.getMovies(Genre.ACTION, favIds) }
    }

    @Test
    fun `GET movies with only_favorites when user has no favorites returns empty list`() = routeTest {
        val userId = UUID.randomUUID()
        every { favoriteService.getFavoritedMovieIds(userId) } returns emptySet()
        every { movieService.getMovies(null, emptySet()) } returns emptyList()

        val response = client.get("/movies?only_favorites=true") {
            header(HttpHeaders.Authorization, "Bearer ${createTestToken(userId)}")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("[]", response.bodyAsText().trim())
    }

    @Test
    fun `GET movies paginated with only_favorites passes favIds to service`() = routeTest {
        val userId = UUID.randomUUID()
        val movieId = UUID.randomUUID()
        val favIds = setOf(movieId)
        every { favoriteService.getFavoritedMovieIds(userId) } returns favIds
        every { movieService.getMoviesPaged(null, 1, 20, favIds) } returns pagedResult(movieId)

        val response = client.get("/movies?page=1&only_favorites=true") {
            header(HttpHeaders.Authorization, "Bearer ${createTestToken(userId)}")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("\"is_favorited\":true"))
        verify { movieService.getMoviesPaged(null, 1, 20, favIds) }
    }

    // endregion
}
