package com.evandhardspace.movie.adminpanel.data

import com.evandhardspace.movie.adminpanel.storage.TokenStorage
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data object Unauthorized : ApiResult<Nothing>()
    data class Error(val message: String) : ApiResult<Nothing>()
}

inline fun <T, R> ApiResult<T>.map(transform: (T) -> R): ApiResult<R> = when (this) {
    is ApiResult.Success -> ApiResult.Success(transform(data))
    is ApiResult.Unauthorized -> ApiResult.Unauthorized
    is ApiResult.Error -> this
}

class ApiClient(
    val baseUrl: String,
    private val tokenStorage: TokenStorage,
) {
    @PublishedApi
    internal val httpClient: HttpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        install(Logging) {
            logger = Logger.SIMPLE
            level = LogLevel.BODY
        }
    }

    @PublishedApi
    internal fun bearerToken(): String? =
        tokenStorage.getAccessToken()?.let { "Bearer $it" }

    @PublishedApi
    internal fun clearTokens() = tokenStorage.clear()

    suspend inline fun <reified T> get(path: String): ApiResult<T> = execute {
        httpClient.get("$baseUrl$path") {
            bearerToken()?.let { header(HttpHeaders.Authorization, it) }
        }
    }

    suspend inline fun <reified T> post(path: String, body: Any? = null): ApiResult<T> = execute {
        httpClient.post("$baseUrl$path") {
            bearerToken()?.let { header(HttpHeaders.Authorization, it) }
            body?.let { contentType(ContentType.Application.Json); setBody(it) }
        }
    }

    suspend inline fun <reified T> put(path: String, body: Any? = null): ApiResult<T> = execute {
        httpClient.put("$baseUrl$path") {
            bearerToken()?.let { header(HttpHeaders.Authorization, it) }
            body?.let { contentType(ContentType.Application.Json); setBody(it) }
        }
    }

    suspend inline fun <reified T> delete(path: String): ApiResult<T> = execute {
        httpClient.delete("$baseUrl$path") {
            bearerToken()?.let { header(HttpHeaders.Authorization, it) }
        }
    }

    @PublishedApi
    internal suspend inline fun <reified T> execute(block: suspend () -> HttpResponse): ApiResult<T> = try {
        val response = block()
        when {
            response.status == HttpStatusCode.Unauthorized -> {
                clearTokens()
                ApiResult.Unauthorized
            }
            response.status.isSuccess() -> ApiResult.Success(response.body())
            else -> ApiResult.Error("HTTP ${response.status.value}")
        }
    } catch (e: Exception) {
        ApiResult.Error(e.message ?: "Network error")
    }
}
