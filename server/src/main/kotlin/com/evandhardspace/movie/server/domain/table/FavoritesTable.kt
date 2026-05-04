package com.evandhardspace.movie.server.domain.table

import org.jetbrains.exposed.v1.core.Table

object FavoritesTable : Table("user_favorites") {
    val userId = reference("user_id", UsersTable)
    val movieId = reference("movie_id", MoviesTable)
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(userId, movieId)
}
