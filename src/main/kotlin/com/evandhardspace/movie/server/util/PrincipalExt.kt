package com.evandhardspace.movie.server.util

import io.ktor.server.auth.jwt.*
import java.util.UUID

fun JWTPrincipal.userId(): UUID = UUID.fromString(payload.subject)
fun JWTPrincipal.userEmail(): String = payload.getClaim("email").asString()
