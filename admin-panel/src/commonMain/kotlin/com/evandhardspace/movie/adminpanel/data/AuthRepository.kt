package com.evandhardspace.movie.adminpanel.data

import com.evandhardspace.movie.adminpanel.storage.TokenStorage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class LoginRequest(val email: String, val password: String)

@Serializable
private data class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
)

class AuthRepository(
    private val apiClient: ApiClient,
    private val tokenStorage: TokenStorage,
) {
    suspend fun login(email: String, password: String): ApiResult<Unit> =
        when (val result = apiClient.post<TokenResponse>("/auth/login", LoginRequest(email, password))) {
            is ApiResult.Success -> {
                tokenStorage.save(result.data.accessToken, result.data.refreshToken)
                ApiResult.Success(Unit)
            }
            is ApiResult.Unauthorized -> ApiResult.Unauthorized
            is ApiResult.Error -> result
        }
}
