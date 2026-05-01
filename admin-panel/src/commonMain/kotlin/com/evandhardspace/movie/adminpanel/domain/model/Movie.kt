package com.evandhardspace.movie.adminpanel.domain.model

data class Movie(
    val id: String,
    val title: String,
    val description: String?,
    val genre: Genre,
    val rating: Double?,
    val photoUrl: String?,
    val createdAt: String,
    val isFavorited: Boolean,
)
