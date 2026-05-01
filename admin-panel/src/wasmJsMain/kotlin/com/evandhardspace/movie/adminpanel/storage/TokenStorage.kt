package com.evandhardspace.movie.adminpanel.storage

import kotlinx.browser.localStorage

actual class TokenStorage {
    actual fun getAccessToken(): String? = localStorage.getItem(KEY_ACCESS_TOKEN)
    actual fun getRefreshToken(): String? = localStorage.getItem(KEY_REFRESH_TOKEN)

    actual fun save(accessToken: String, refreshToken: String) {
        localStorage.setItem(KEY_ACCESS_TOKEN, accessToken)
        localStorage.setItem(KEY_REFRESH_TOKEN, refreshToken)
    }

    actual fun clear() {
        localStorage.removeItem(KEY_ACCESS_TOKEN)
        localStorage.removeItem(KEY_REFRESH_TOKEN)
    }

    private companion object {
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
    }
}
