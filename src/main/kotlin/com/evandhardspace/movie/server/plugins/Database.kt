package com.evandhardspace.movie.server.plugins

import io.ktor.server.application.*
import org.jetbrains.exposed.v1.jdbc.Database

fun Application.configureDatabase() {
    val url = System.getenv("DATABASE_URL") ?: error("DATABASE_URL not set")
    val user = System.getenv("DATABASE_USER") ?: error("DATABASE_USER not set")
    val password = System.getenv("DATABASE_PASSWORD") ?: error("DATABASE_PASSWORD not set")

    Database.connect(
        url = url,
        driver = "org.postgresql.Driver",
        user = user,
        password = password,
    )
}
