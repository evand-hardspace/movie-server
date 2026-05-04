package com.evandhardspace.movie.server.domain.service

import com.evandhardspace.movie.server.domain.model.Genre
import com.evandhardspace.movie.server.domain.model.Movie
import com.evandhardspace.movie.server.domain.table.MoviesTable
import com.evandhardspace.movie.server.domain.table.UsersTable
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.util.UUID
import kotlin.time.Instant

class MovieService {
    fun getMovies(genre: Genre?, movieIds: Set<UUID>? = null): List<Movie> = transaction {
        if (movieIds != null && movieIds.isEmpty()) return@transaction emptyList()
        val entityIds = movieIds?.map { EntityID(it, MoviesTable) }
        MoviesTable.selectAll()
            .let { q ->
                when {
                    genre != null && entityIds != null -> q.where { (MoviesTable.genre eq genre) and (MoviesTable.id inList entityIds) }
                    genre != null -> q.where { MoviesTable.genre eq genre }
                    entityIds != null -> q.where { MoviesTable.id inList entityIds }
                    else -> q
                }
            }
            .map { it.toMovie() }
    }

    fun getMovieById(id: UUID): Movie? = transaction {
        MoviesTable.selectAll()
            .where { MoviesTable.id eq id }
            .singleOrNull()
            ?.toMovie()
    }

    fun createMovie(
        title: String,
        description: String?,
        genre: Genre,
        rating: Double?,
        photoUrl: String?,
        createdBy: UUID,
    ): Movie {
        val now = System.currentTimeMillis()
        return transaction {
            val insertedId = MoviesTable.insertAndGetId {
                it[this.title] = title
                it[this.description] = description
                it[this.genre] = genre
                it[this.rating] = rating?.toBigDecimal()
                it[this.photoUrl] = photoUrl
                it[this.createdBy] = EntityID(createdBy, UsersTable)
                it[this.createdAt] = now
                it[this.updatedAt] = now
            }
            Movie(
                id = insertedId.value.toString(),
                title = title,
                description = description,
                genre = genre,
                rating = rating,
                photoUrl = photoUrl,
                createdAt = Instant.fromEpochMilliseconds(now),
            )
        }
    }

    fun updateMovie(
        id: UUID,
        title: String,
        description: String?,
        genre: Genre,
        rating: Double?,
        photoUrl: String?,
    ): Movie? = transaction {
        val now = System.currentTimeMillis()
        val updated = MoviesTable.update({ MoviesTable.id eq id }) {
            it[this.title] = title
            it[this.description] = description
            it[this.genre] = genre
            it[this.rating] = rating?.toBigDecimal()
            it[this.photoUrl] = photoUrl
            it[this.updatedAt] = now
        }
        if (updated == 0) return@transaction null
        MoviesTable.selectAll()
            .where { MoviesTable.id eq id }
            .singleOrNull()
            ?.toMovie()
    }

    fun deleteMovie(id: UUID): Boolean = transaction {
        MoviesTable.deleteWhere { MoviesTable.id eq id } > 0
    }

    fun hasMovies(): Boolean = transaction {
        MoviesTable.selectAll().count() > 0
    }

    fun getMoviesPaged(genre: Genre?, page: Int, pageSize: Int, movieIds: Set<UUID>? = null): PagedMovies {
        if (movieIds != null && movieIds.isEmpty()) return PagedMovies(emptyList(), page, pageSize, 0L, 1)
        return transaction {
            val entityIds = movieIds?.map { EntityID(it, MoviesTable) }
            val base = MoviesTable.selectAll()
                .let { q ->
                    when {
                        genre != null && entityIds != null -> q.where { (MoviesTable.genre eq genre) and (MoviesTable.id inList entityIds) }
                        genre != null -> q.where { MoviesTable.genre eq genre }
                        entityIds != null -> q.where { MoviesTable.id inList entityIds }
                        else -> q
                    }
                }
            val total = base.count()
            val items = base
                .limit(pageSize)
                .offset((page - 1).toLong() * pageSize)
                .map { it.toMovie() }
            PagedMovies(
                items = items,
                page = page,
                pageSize = pageSize,
                total = total,
                totalPages = ((total + pageSize - 1) / pageSize).toInt().coerceAtLeast(1),
            )
        }
    }

    private fun ResultRow.toMovie() = Movie(
        id = this[MoviesTable.id].value.toString(),
        title = this[MoviesTable.title],
        description = this[MoviesTable.description],
        genre = this[MoviesTable.genre],
        rating = this[MoviesTable.rating]?.toDouble(),
        photoUrl = this[MoviesTable.photoUrl],
        createdAt = Instant.fromEpochMilliseconds(this[MoviesTable.createdAt]),
    )
}

data class PagedMovies(
    val items: List<Movie>,
    val page: Int,
    val pageSize: Int,
    val total: Long,
    val totalPages: Int,
)
