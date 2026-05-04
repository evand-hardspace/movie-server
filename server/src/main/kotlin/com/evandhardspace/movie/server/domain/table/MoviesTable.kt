package com.evandhardspace.movie.server.domain.table

import com.evandhardspace.movie.server.domain.model.Genre
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable

object MoviesTable : UUIDTable("movies") {
    val title = text("title")
    val description = text("description").nullable()
    val genre = enumeration<Genre>("genre")
    val rating = decimal("rating", 3, 1).nullable()
    val photoUrl = text("photo_url").nullable()
    val createdBy = reference("created_by", UsersTable)
    val createdAt = long("created_at")
    val updatedAt = long("updated_at")
}
