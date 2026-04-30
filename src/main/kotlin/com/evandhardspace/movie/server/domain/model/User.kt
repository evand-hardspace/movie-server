package com.evandhardspace.movie.server.domain.model

import java.util.UUID

data class User(
    val id: UUID,
    val email: String,
    val isAdmin: Boolean,
)
