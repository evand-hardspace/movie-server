package com.evandhardspace.movie.server.plugins

import com.evandhardspace.movie.server.domain.model.UserRole
import com.evandhardspace.movie.server.domain.service.AuthService
import com.evandhardspace.movie.server.domain.service.UserService
import io.ktor.server.application.*
import io.ktor.server.plugins.di.*

fun Application.configureSeed() {
    val adminEmail = System.getenv("SEED_ADMIN_EMAIL") ?: return
    val adminPassword = System.getenv("SEED_ADMIN_PASSWORD") ?: return

    val userService: UserService by dependencies
    val authService: AuthService by dependencies

    if (!userService.hasUsers()) {
        val result = authService.signUp(adminEmail, adminPassword)
        userService.updateRole(result.userId, UserRole.SUPER_ADMIN)
        log.info("Seeded super_admin: $adminEmail")
    }
}
