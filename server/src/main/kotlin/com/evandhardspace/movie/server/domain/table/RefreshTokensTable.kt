package com.evandhardspace.movie.server.domain.table

import org.jetbrains.exposed.v1.core.Table

object RefreshTokensTable : Table("refresh_tokens") {
    val token = text("token")
    val userId = reference("user_id", UsersTable)
    val expiresAt = long("expires_at")
    val isRevoked = bool("is_revoked").default(false)

    override val primaryKey = PrimaryKey(token)
}
