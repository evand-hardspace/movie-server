package com.evandhardspace.movie.server.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class Genre {
    ACTION, COMEDY, DRAMA, HORROR,
    THRILLER, ROMANCE, SCI_FI, DOCUMENTARY, ANIMATION
}
