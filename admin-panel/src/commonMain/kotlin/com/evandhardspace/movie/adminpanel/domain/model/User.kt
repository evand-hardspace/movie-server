package com.evandhardspace.movie.adminpanel.domain.model

data class User(
    val id: String,
    val email: String,
    val role: UserRole,
)
