package com.evandhardspace.movie.server.domain.service

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

class AuthService(private val supabaseUrl: String, private val anonKey: String) {

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) { json() }
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.BODY
        }
    }

    private fun HttpRequestBuilder.supabaseHeaders() {
        header("apikey", anonKey)
        contentType(ContentType.Application.Json)
    }

    suspend fun signIn(email: String, password: String): AuthResult =
        client.post("$supabaseUrl/auth/v1/token?grant_type=password") {
            supabaseHeaders()
            setBody(mapOf("email" to email, "password" to password))
        }.toAuthResult()

    suspend fun signUp(email: String, password: String): AuthResult =
        client.post("$supabaseUrl/auth/v1/signup") {
            supabaseHeaders()
            setBody(mapOf("email" to email, "password" to password))
        }.toAuthResult()

    suspend fun refresh(refreshToken: String): AuthTokenResponse {
        val resp = client.post("$supabaseUrl/auth/v1/token?grant_type=refresh_token") {
            supabaseHeaders()
            setBody(mapOf("refresh_token" to refreshToken))
        }
        if (!resp.status.isSuccess()) throw AuthException(resp.body<SupabaseError>().message(), resp.status)
        return resp.body<SupabaseSession>().toTokenResponse()
    }

    private suspend fun HttpResponse.toAuthResult(): AuthResult {
        if (!status.isSuccess()) throw AuthException(body<SupabaseError>().message(), status)
        val session = body<SupabaseSession>()
        return AuthResult(session.toTokenResponse(), UUID.fromString(session.user.id), session.user.email)
    }
}

@Serializable
private data class SupabaseSession(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("token_type") val tokenType: String,
    @SerialName("expires_in") val expiresIn: Int,
    val user: SupabaseUser,
) {
    fun toTokenResponse() = AuthTokenResponse(accessToken, refreshToken, tokenType, expiresIn)
}

@Serializable
private data class SupabaseUser(val id: String, val email: String)

@Serializable
private data class SupabaseError(
    val code: Int? = null,
    val error: String? = null,
    @SerialName("error_code") val errorCode: String? = null,
    @SerialName("error_description") val errorDescription: String? = null,
    val msg: String? = null,
    val message: String? = null,
    val hint: String? = null,
) {
    fun message() = errorDescription ?: msg ?: message ?: error ?: errorCode ?: "Authentication failed"
}

@Serializable
data class AuthTokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("token_type") val tokenType: String,
    @SerialName("expires_in") val expiresIn: Int,
)

data class AuthResult(val tokens: AuthTokenResponse, val userId: UUID, val email: String)

class AuthException(message: String, val status: HttpStatusCode) : Exception(message)
