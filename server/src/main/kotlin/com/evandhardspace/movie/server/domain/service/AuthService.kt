package com.evandhardspace.movie.server.domain.service

import at.favre.lib.crypto.bcrypt.BCrypt
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.evandhardspace.movie.server.domain.table.RefreshTokensTable
import com.evandhardspace.movie.server.domain.table.UsersTable
import io.ktor.http.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.util.Date
import java.util.UUID

private const val ACCESS_TOKEN_TTL_MS = 60 * 60 * 1000L        // 1 hour
private const val REFRESH_TOKEN_TTL_MS = 30 * 24 * 60 * 60 * 1000L // 30 days

class AuthService(
    private val jwtSecret: String,
    private val userService: UserService,
) {
    fun signUp(email: String, password: String): AuthResult {
        if (userService.findByEmail(email) != null)
            throw AuthException("Email already registered", HttpStatusCode.UnprocessableEntity)

        val hash = BCrypt.withDefaults().hashToString(12, password.toCharArray())
        val userId = userService.createUser(email, hash)
        val tokens = issueTokens(userId, email)
        return AuthResult(tokens, userId, email)
    }

    fun signIn(email: String, password: String): AuthResult {
        val credentials = userService.findByEmail(email)
            ?: throw AuthException("Invalid email or password", HttpStatusCode.Unauthorized)

        val verified = BCrypt.verifyer().verify(password.toCharArray(), credentials.passwordHash).verified
        if (!verified) throw AuthException("Invalid email or password", HttpStatusCode.Unauthorized)

        val tokens = issueTokens(credentials.id, credentials.email)
        return AuthResult(tokens, credentials.id, credentials.email)
    }

    fun refresh(refreshToken: String): AuthTokenResponse {
        val row = transaction {
            RefreshTokensTable.selectAll()
                .where { RefreshTokensTable.token eq refreshToken }
                .singleOrNull()
        } ?: throw AuthException("Refresh token not found", HttpStatusCode.Unauthorized)

        val isRevoked = row[RefreshTokensTable.isRevoked]
        val expiresAt = row[RefreshTokensTable.expiresAt]
        if (isRevoked || expiresAt < System.currentTimeMillis())
            throw AuthException("Refresh token invalid or expired", HttpStatusCode.Unauthorized)

        val userId = row[RefreshTokensTable.userId].value

        transaction {
            RefreshTokensTable.update({ RefreshTokensTable.token eq refreshToken }) {
                it[RefreshTokensTable.isRevoked] = true
            }
        }

        val email = transaction {
            UsersTable.selectAll()
                .where { UsersTable.id eq userId }
                .single()[UsersTable.email]
        }

        return issueTokens(userId, email)
    }

    private fun issueTokens(userId: UUID, email: String): AuthTokenResponse {
        val accessToken = JWT.create()
            .withSubject(userId.toString())
            .withClaim("email", email)
            .withAudience("authenticated")
            .withExpiresAt(Date(System.currentTimeMillis() + ACCESS_TOKEN_TTL_MS))
            .sign(Algorithm.HMAC256(jwtSecret))

        val refreshToken = UUID.randomUUID().toString()
        transaction {
            RefreshTokensTable.insert {
                it[token] = refreshToken
                it[this.userId] = EntityID(userId, UsersTable)
                it[expiresAt] = System.currentTimeMillis() + REFRESH_TOKEN_TTL_MS
                it[isRevoked] = false
            }
        }

        return AuthTokenResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
            tokenType = "Bearer",
            expiresIn = (ACCESS_TOKEN_TTL_MS / 1000).toInt(),
        )
    }
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
