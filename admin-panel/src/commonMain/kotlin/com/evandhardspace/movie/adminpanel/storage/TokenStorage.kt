package com.evandhardspace.movie.adminpanel.storage

expect class TokenStorage() {
    fun getAccessToken(): String?
    fun getRefreshToken(): String?
    fun save(accessToken: String, refreshToken: String)
    fun clear()
}
