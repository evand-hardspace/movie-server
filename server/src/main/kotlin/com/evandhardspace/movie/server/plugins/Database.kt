package com.evandhardspace.movie.server.plugins

import com.evandhardspace.movie.server.domain.table.FavoritesTable
import com.evandhardspace.movie.server.domain.table.MoviesTable
import com.evandhardspace.movie.server.domain.table.RefreshTokensTable
import com.evandhardspace.movie.server.domain.table.UsersTable
import io.ktor.server.application.*
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

fun Application.configureDatabase(
    url: String = "jdbc:sqlite:${System.getenv("SQLITE_FILE") ?: "./movies.db"}",
) {
    Database.connect(
        url = url,
        driver = "org.sqlite.JDBC",
    )

    transaction {
        SchemaUtils.create(UsersTable, RefreshTokensTable, MoviesTable, FavoritesTable)
    }
}
