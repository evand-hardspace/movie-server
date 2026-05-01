package com.evandhardspace.movie.adminpanel.data

import com.evandhardspace.movie.adminpanel.domain.model.User
import com.evandhardspace.movie.adminpanel.domain.model.UserRole
import kotlinx.serialization.Serializable

@Serializable
private data class UserDto(
    val id: String,
    val email: String,
    val role: UserRole,
)

@Serializable
private data class UpdateRoleRequest(val role: UserRole)

private fun UserDto.toDomain() = User(id = id, email = email, role = role)

class UserRepository(private val apiClient: ApiClient) {
    suspend fun getMe(): ApiResult<User> =
        apiClient.get<UserDto>("/users/me").map { it.toDomain() }

    suspend fun getUsers(): ApiResult<List<User>> =
        apiClient.get<List<UserDto>>("/users").map { list -> list.map { it.toDomain() } }

    suspend fun updateRole(id: String, role: UserRole): ApiResult<Unit> =
        apiClient.put<Unit>("/users/$id/role", UpdateRoleRequest(role))
}
