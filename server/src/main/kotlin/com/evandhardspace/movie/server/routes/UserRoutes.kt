package com.evandhardspace.movie.server.routes

import com.evandhardspace.movie.server.domain.model.UserRole
import com.evandhardspace.movie.server.domain.service.UserService
import com.evandhardspace.movie.server.util.userId
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class UpdateRoleRequest(val role: UserRole)

@Serializable
data class UserResponse(val id: String, val email: String, val role: UserRole)

fun Route.userRoutes(userService: UserService) {
    authenticate("auth-jwt") {
        get("/users/me") {
            val principal = call.principal<JWTPrincipal>()!!
            val user = userService.getUser(principal.userId())
                ?: return@get call.respond(HttpStatusCode.NotFound, mapOf("error" to "User not found"))
            call.respond(UserResponse(user.id.toString(), user.email, user.role))
        }

        get("/users") {
            val principal = call.principal<JWTPrincipal>()!!
            if (!userService.isSuperAdmin(principal.userId()))
                return@get call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Super admin access required"))
            val users = userService.getAllUsers().map { UserResponse(it.id.toString(), it.email, it.role) }
            call.respond(users)
        }

        put("/users/{id}/role") {
            val principal = call.principal<JWTPrincipal>()!!
            val callerId = principal.userId()
            if (!userService.isSuperAdmin(callerId))
                return@put call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Super admin access required"))
            val targetId = parseUuid(call.pathParameters["id"])
                ?: return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid user ID"))
            if (targetId == callerId)
                return@put call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Cannot change your own role"))
            val req = call.receive<UpdateRoleRequest>()
            if (userService.updateRole(targetId, req.role)) call.respond(HttpStatusCode.OK)
            else call.respond(HttpStatusCode.NotFound)
        }
    }
}
