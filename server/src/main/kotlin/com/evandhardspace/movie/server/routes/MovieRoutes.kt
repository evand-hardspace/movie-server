package com.evandhardspace.movie.server.routes

import com.evandhardspace.movie.server.domain.model.Genre
import com.evandhardspace.movie.server.domain.service.FavoriteService
import com.evandhardspace.movie.server.domain.service.MovieService
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

fun Route.movieRoutes(movieService: MovieService, userService: UserService, favoriteService: FavoriteService) {
    authenticate("auth-jwt", optional = true) {
        get("/movies") {
            val genreParam = call.request.queryParameters["genre"]
            val genre = if (genreParam != null) {
                try { Genre.valueOf(genreParam) } catch (e: IllegalArgumentException) {
                    return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid genre: $genreParam"))
                }
            } else null
            val movies = movieService.getMovies(genre)
            val principal = call.principal<JWTPrincipal>()
            if (principal != null) {
                val favIds = favoriteService.getFavoritedMovieIds(principal.userId())
                call.respond(movies.map { it.copy(isFavorited = UUID.fromString(it.id) in favIds) })
            } else {
                call.respond(movies)
            }
        }

        get("/movies/{id}") {
            val id = parseUuid(call.pathParameters["id"]) ?: return@get call.respond(
                HttpStatusCode.BadRequest, mapOf("error" to "Invalid movie ID")
            )
            val movie = movieService.getMovieById(id) ?: return@get call.respond(HttpStatusCode.NotFound)
            val principal = call.principal<JWTPrincipal>()
            if (principal != null) {
                call.respond(movie.copy(isFavorited = favoriteService.isFavorited(principal.userId(), id)))
            } else {
                call.respond(movie)
            }
        }
    }

    authenticate("auth-jwt") {
        post("/movies") {
            val principal = call.principal<JWTPrincipal>()!!
            val userId = principal.userId()
            if (!userService.isAdmin(userId)) return@post call.respond(HttpStatusCode.Forbidden)
            val request = call.receive<MovieRequest>()
            val movie = movieService.createMovie(
                title = request.title,
                description = request.description,
                genre = request.genre,
                rating = request.rating,
                photoUrl = request.photoUrl,
                createdBy = userId,
            )
            call.respond(HttpStatusCode.Created, movie)
        }

        put("/movies/{id}") {
            val principal = call.principal<JWTPrincipal>()!!
            val userId = principal.userId()
            if (!userService.isAdmin(userId)) return@put call.respond(HttpStatusCode.Forbidden)
            val id = parseUuid(call.pathParameters["id"]) ?: return@put call.respond(
                HttpStatusCode.BadRequest, mapOf("error" to "Invalid movie ID")
            )
            val request = call.receive<MovieRequest>()
            val movie = movieService.updateMovie(
                id = id,
                title = request.title,
                description = request.description,
                genre = request.genre,
                rating = request.rating,
                photoUrl = request.photoUrl,
            )
            if (movie != null) call.respond(movie) else call.respond(HttpStatusCode.NotFound)
        }

        delete("/movies/{id}") {
            val principal = call.principal<JWTPrincipal>()!!
            val userId = principal.userId()
            if (!userService.isAdmin(userId)) return@delete call.respond(HttpStatusCode.Forbidden)
            val id = parseUuid(call.pathParameters["id"]) ?: return@delete call.respond(
                HttpStatusCode.BadRequest, mapOf("error" to "Invalid movie ID")
            )
            val deleted = movieService.deleteMovie(id)
            if (deleted) call.respond(HttpStatusCode.NoContent) else call.respond(HttpStatusCode.NotFound)
        }
    }
}

internal fun parseUuid(value: String?): UUID? = value?.let {
    try { UUID.fromString(it) } catch (e: IllegalArgumentException) { null }
}
