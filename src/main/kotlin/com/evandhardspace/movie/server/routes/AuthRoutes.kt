package com.evandhardspace.movie.server.routes

import com.evandhardspace.movie.server.domain.service.UserService
import com.evandhardspace.movie.server.util.userEmail
import com.evandhardspace.movie.server.util.userId
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.authRoutes(userService: UserService) {
    authenticate("auth-jwt") {
        post("/auth/sync") {
            val principal = call.principal<JWTPrincipal>()!!
            userService.upsertUser(principal.userId(), principal.userEmail())
            call.respond(HttpStatusCode.OK)
        }
    }
}
