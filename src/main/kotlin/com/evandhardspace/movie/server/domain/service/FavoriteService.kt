package com.evandhardspace.movie.server.domain.service

import com.evandhardspace.movie.server.domain.table.FavoritesTable
import com.evandhardspace.movie.server.domain.table.MoviesTable
import com.evandhardspace.movie.server.domain.table.UsersTable
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.OffsetDateTime
import java.util.UUID

class FavoriteService {
    fun addFavorite(userId: UUID, movieId: UUID) = transaction {
        val exists = FavoritesTable.selectAll()
            .where { (FavoritesTable.userId eq userId) and (FavoritesTable.movieId eq movieId) }
            .singleOrNull() != null
        if (!exists) {
            FavoritesTable.insert {
                it[this.userId] = EntityID(userId, UsersTable)
                it[this.movieId] = EntityID(movieId, MoviesTable)
                it[createdAt] = OffsetDateTime.now()
            }
        }
    }

    fun removeFavorite(userId: UUID, movieId: UUID) = transaction {
        FavoritesTable.deleteWhere {
            (FavoritesTable.userId eq userId) and (FavoritesTable.movieId eq movieId)
        }
    }

    fun isFavorited(userId: UUID, movieId: UUID): Boolean = transaction {
        FavoritesTable.selectAll()
            .where { (FavoritesTable.userId eq userId) and (FavoritesTable.movieId eq movieId) }
            .singleOrNull() != null
    }

    fun getFavoritedMovieIds(userId: UUID): Set<UUID> = transaction {
        FavoritesTable.selectAll()
            .where { FavoritesTable.userId eq userId }
            .map { it[FavoritesTable.movieId].value }
            .toSet()
    }
}
