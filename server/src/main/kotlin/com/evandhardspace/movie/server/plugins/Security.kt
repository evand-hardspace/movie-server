package com.evandhardspace.movie.server.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.di.dependencies
import io.ktor.server.response.*

suspend fun Application.configureSecurity() {
    val secret: String = dependencies.resolve("jwt.secret")
    val audience: String = dependencies.resolve("jwt.audience")

    val verifier = JWT.require(Algorithm.HMAC256(secret))
        .withAudience(audience)
        .build()

    authentication {
        jwt("auth-jwt") {
            realm = "movie-server"
            verifier(verifier)
            validate { credential ->
                if (credential.payload.audience.contains(audience)) JWTPrincipal(credential.payload) else null
            }
            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token is not valid or has expired"))
            }
        }
    }
}
