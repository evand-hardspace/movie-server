package com.evandhardspace.movie.server.routes

import com.evandhardspace.movie.server.domain.service.FavoriteService
import com.evandhardspace.movie.server.util.userId
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.favoriteRoutes(favoriteService: FavoriteService) {
    authenticate("auth-jwt") {
        post("/movies/{id}/favorite") {
            val principal = call.principal<JWTPrincipal>()!!
            val movieId = parseUuid(call.pathParameters["id"]) ?: return@post call.respond(
                HttpStatusCode.BadRequest, mapOf("error" to "Invalid movie ID")
            )
            favoriteService.addFavorite(principal.userId(), movieId)
            call.respond(HttpStatusCode.OK)
        }

        delete("/movies/{id}/favorite") {
            val principal = call.principal<JWTPrincipal>()!!
            val movieId = parseUuid(call.pathParameters["id"]) ?: return@delete call.respond(
                HttpStatusCode.BadRequest, mapOf("error" to "Invalid movie ID")
            )
            favoriteService.removeFavorite(principal.userId(), movieId)
            call.respond(HttpStatusCode.OK)
        }
    }
}
