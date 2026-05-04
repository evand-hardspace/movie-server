package com.evandhardspace.movie.server.domain.table

import com.evandhardspace.movie.server.domain.model.UserRole
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable

object UsersTable : UUIDTable("users") {
    val email = text("email")
    val passwordHash = text("password_hash")
    val role = enumeration<UserRole>("role")
    val createdAt = long("created_at")
}
