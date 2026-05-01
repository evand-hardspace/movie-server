package com.evandhardspace.movie.adminpanel.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class UserRole {
    @SerialName("user") USER,
    @SerialName("admin") ADMIN,
    @SerialName("super_admin") SUPER_ADMIN,
}
