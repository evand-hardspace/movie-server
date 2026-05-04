package com.evandhardspace.movie.server.routes

import com.evandhardspace.movie.server.domain.model.Genre
import com.evandhardspace.movie.server.domain.model.Movie
import com.evandhardspace.movie.server.domain.service.MovieFacade
import com.evandhardspace.movie.server.domain.service.PagedMovies
import com.evandhardspace.movie.server.domain.service.UserService
import com.evandhardspace.movie.server.util.userId
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class MovieRequest(
    val title: String,
    val description: String? = null,
    val genre: Genre,
    val rating: Double? = null,
    @SerialName("photo_url") val photoUrl: String? = null,
)

@Serializable
data class PagedMoviesResponse(
    val items: List<Movie>,
    val page: Int,
    @SerialName("page_size") val pageSize: Int,
    val total: Long,
    @SerialName("total_pages") val totalPages: Int,
)

private data class MovieListParams(
    val genre: Genre?,
    val onlyFavorites: Boolean,
    val page: Int?,
    val pageSize: Int,
)

private fun Parameters.parseMovieListParams(): Result<MovieListParams> {
    val genreParam = get("genre")
    val genre = if (genreParam != null) {
        runCatching { Genre.valueOf(genreParam) }.getOrElse {
            return Result.failure(IllegalArgumentException("Invalid genre: $genreParam"))
        }
    } else null
    return Result.success(
        MovieListParams(
            genre = genre,
            onlyFavorites = get("filterBy") == "favorite",
            page = get("page")?.toIntOrNull()?.coerceAtLeast(1),
            pageSize = get("page_size")?.toIntOrNull()?.coerceIn(1, 100) ?: 20,
        )
    )
}

private fun PagedMovies.toResponse() = PagedMoviesResponse(items, page, pageSize, total, totalPages)

private suspend fun RoutingCall.requireAdminId(userService: UserService): UUID? {
    val userId = principal<JWTPrincipal>()!!.userId()
    return if (userService.isAdmin(userId)) userId
    else { respond(HttpStatusCode.Forbidden); null }
}

fun Route.movieRoutes(facade: MovieFacade, userService: UserService) {
    authenticate("auth-jwt", optional = true) {
        get("/movies") {
            val params = call.request.queryParameters.parseMovieListParams().getOrElse { e ->
                return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
            }
            val principal = call.principal<JWTPrincipal>()
            if (params.onlyFavorites && principal == null) {
                return@get call.respond(
                    HttpStatusCode.Unauthorized,
                    mapOf("error" to "Authentication required for favorites filter"),
                )
            }
            val userId = principal?.userId()
            if (params.page != null) {
                call.respond(facade.getMoviesPaged(params.genre, params.page, params.pageSize, userId, params.onlyFavorites).toResponse())
            } else {
                call.respond(facade.getMovies(params.genre, userId, params.onlyFavorites))
            }
        }

        get("/movies/{id}") {
            val id = parseUuid(call.pathParameters["id"]) ?: return@get call.respond(
                HttpStatusCode.BadRequest, mapOf("error" to "Invalid movie ID")
            )
            val movie = facade.getMovieById(id, call.principal<JWTPrincipal>()?.userId())
                ?: return@get call.respond(HttpStatusCode.NotFound)
            call.respond(movie)
        }
    }

    authenticate("auth-jwt") {
        post("/movies") {
            val userId = call.requireAdminId(userService) ?: return@post
            val request = call.receive<MovieRequest>()
            call.respond(
                HttpStatusCode.Created,
                facade.createMovie(request.title, request.description, request.genre, request.rating, request.photoUrl, userId),
            )
        }

        put("/movies/{id}") {
            call.requireAdminId(userService) ?: return@put
            val id = parseUuid(call.pathParameters["id"]) ?: return@put call.respond(
                HttpStatusCode.BadRequest, mapOf("error" to "Invalid movie ID")
            )
            val request = call.receive<MovieRequest>()
            val movie = facade.updateMovie(id, request.title, request.description, request.genre, request.rating, request.photoUrl)
            if (movie != null) call.respond(movie) else call.respond(HttpStatusCode.NotFound)
        }

        delete("/movies/{id}") {
            call.requireAdminId(userService) ?: return@delete
            val id = parseUuid(call.pathParameters["id"]) ?: return@delete call.respond(
                HttpStatusCode.BadRequest, mapOf("error" to "Invalid movie ID")
            )
            if (facade.deleteMovie(id)) call.respond(HttpStatusCode.NoContent) else call.respond(HttpStatusCode.NotFound)
        }
    }
}

internal fun parseUuid(value: String?): UUID? = value?.let {
    try { UUID.fromString(it) } catch (e: IllegalArgumentException) { null }
}
