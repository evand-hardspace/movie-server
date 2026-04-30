package com.evandhardspace.movie.server.domain.model

import kotlin.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Movie(
    val id: String,
    val title: String,
    val description: String?,
    val genre: Genre,
    val rating: Double?,
    @SerialName("photo_url") val photoUrl: String?,
    @SerialName("created_at") val createdAt: Instant,
)
