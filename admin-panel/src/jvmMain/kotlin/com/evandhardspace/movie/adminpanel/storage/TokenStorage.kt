package com.evandhardspace.movie.adminpanel.storage

actual class TokenStorage {
    private var accessToken: String? = null
    private var refreshToken: String? = null

    actual fun getAccessToken(): String? = accessToken
    actual fun getRefreshToken(): String? = refreshToken

    actual fun save(accessToken: String, refreshToken: String) {
        this.accessToken = accessToken
        this.refreshToken = refreshToken
    }

    actual fun clear() {
        accessToken = null
        refreshToken = null
    }
}
