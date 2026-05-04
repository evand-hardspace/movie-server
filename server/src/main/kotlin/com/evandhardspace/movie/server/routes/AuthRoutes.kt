package com.evandhardspace.movie.server.routes

import com.evandhardspace.movie.server.domain.service.AuthException
import com.evandhardspace.movie.server.domain.service.AuthService
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthRequest(val email: String, val password: String)

@Serializable
data class RefreshRequest(@SerialName("refresh_token") val refreshToken: String)

fun Route.authRoutes(authService: AuthService) {
    post("/auth/register") {
        val req = call.receive<AuthRequest>()
        try {
            val result = authService.signUp(req.email, req.password)
            call.respond(HttpStatusCode.Created, result.tokens)
        } catch (e: AuthException) {
            call.respond(e.status, mapOf("error" to e.message))
        }
    }

    post("/auth/login") {
        val req = call.receive<AuthRequest>()
        try {
            val result = authService.signIn(req.email, req.password)
            call.respond(result.tokens)
        } catch (e: AuthException) {
            call.respond(e.status, mapOf("error" to e.message))
        }
    }

    post("/auth/refresh") {
        val req = call.receive<RefreshRequest>()
        try {
            call.respond(authService.refresh(req.refreshToken))
        } catch (e: AuthException) {
            call.respond(e.status, mapOf("error" to e.message))
        }
    }
}
