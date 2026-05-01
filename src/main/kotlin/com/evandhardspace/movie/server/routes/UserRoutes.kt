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

@Serializable
data class UpdateRoleRequest(val role: UserRole)

fun Route.userRoutes(userService: UserService) {
    authenticate("auth-jwt") {
        put("/users/{id}/role") {
            val principal = call.principal<JWTPrincipal>()!!
            if (!userService.isSuperAdmin(principal.userId()))
                return@put call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Super admin access required"))
            val targetId = parseUuid(call.pathParameters["id"])
                ?: return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid user ID"))
            val req = call.receive<UpdateRoleRequest>()
            if (userService.updateRole(targetId, req.role)) call.respond(HttpStatusCode.OK)
            else call.respond(HttpStatusCode.NotFound)
        }
    }
}
