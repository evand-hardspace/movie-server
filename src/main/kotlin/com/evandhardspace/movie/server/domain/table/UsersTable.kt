package com.evandhardspace.movie.server.domain.table

import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.datetime.timestampWithTimeZone

object UsersTable : UUIDTable("users") {
    val email = text("email")
    val isAdmin = bool("is_admin")
    val createdAt = timestampWithTimeZone("created_at")
}
