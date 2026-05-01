package com.evandhardspace.movie.server.domain.table

import com.evandhardspace.movie.server.domain.model.UserRole
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.datetime.timestampWithTimeZone
import org.postgresql.util.PGobject

object UsersTable : UUIDTable("users") {
    val email = text("email")
    val role = customEnumeration(
        name = "role",
        sql = "user_role",
        fromDb = { UserRole.valueOf((it as String).uppercase()) },
        toDb = { PGobject().apply { type = "user_role"; value = it.name.lowercase() } },
    )
    val createdAt = timestampWithTimeZone("created_at")
}
