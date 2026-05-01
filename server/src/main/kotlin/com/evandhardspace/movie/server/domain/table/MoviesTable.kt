package com.evandhardspace.movie.server.domain.table

import com.evandhardspace.movie.server.domain.model.Genre
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.datetime.timestampWithTimeZone
import org.postgresql.util.PGobject

object MoviesTable : UUIDTable("movies") {
    val title = text("title")
    val description = text("description").nullable()
    val genre = customEnumeration(
        name = "genre",
        sql = "genre",
        fromDb = { Genre.valueOf(it as String) },
        toDb = { PGobject().apply { type = "genre"; value = it.name } },
    )
    val rating = decimal("rating", 3, 1).nullable()
    val photoUrl = text("photo_url").nullable()
    val createdBy = reference("created_by", UsersTable)
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")
}
