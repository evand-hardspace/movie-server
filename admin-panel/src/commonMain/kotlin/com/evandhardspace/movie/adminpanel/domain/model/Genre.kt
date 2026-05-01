package com.evandhardspace.movie.adminpanel.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class Genre {
    ACTION, COMEDY, DRAMA, HORROR,
    THRILLER, ROMANCE, SCI_FI, DOCUMENTARY, ANIMATION
}
