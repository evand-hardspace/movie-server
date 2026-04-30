package com.evandhardspace.movie.server.routes

import com.evandhardspace.movie.server.configureTestApp
import com.evandhardspace.movie.server.createTestToken
import com.evandhardspace.movie.server.domain.model.Genre
import com.evandhardspace.movie.server.domain.service.FavoriteService
import com.evandhardspace.movie.server.domain.service.MovieService
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
}
