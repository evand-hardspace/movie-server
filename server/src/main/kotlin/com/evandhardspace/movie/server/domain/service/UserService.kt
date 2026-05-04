package com.evandhardspace.movie.server.domain.service

import com.evandhardspace.movie.server.domain.model.User
import com.evandhardspace.movie.server.domain.model.UserRole
import com.evandhardspace.movie.server.domain.table.UsersTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.util.UUID

data class UserCredentials(
    val id: UUID,
    val email: String,
    val passwordHash: String,
    val role: UserRole,
)

class UserService {
    fun createUser(email: String, passwordHash: String): UUID = transaction {
        val newId = UUID.randomUUID()
        UsersTable.insert {
            it[id] = EntityID(newId, UsersTable)
            it[this.email] = email
            it[this.passwordHash] = passwordHash
            it[role] = UserRole.USER
            it[createdAt] = System.currentTimeMillis()
        }
        newId
    }

    fun findByEmail(email: String): UserCredentials? = transaction {
        UsersTable.selectAll()
            .where { UsersTable.email eq email }
            .singleOrNull()
            ?.let { row ->
                UserCredentials(
                    id = row[UsersTable.id].value,
                    email = row[UsersTable.email],
                    passwordHash = row[UsersTable.passwordHash],
                    role = row[UsersTable.role],
                )
            }
    }

    fun isAdmin(userId: UUID): Boolean = transaction {
        val userRole = UsersTable.selectAll()
            .where { UsersTable.id eq userId }
            .singleOrNull()
            ?.get(UsersTable.role) ?: return@transaction false
        userRole == UserRole.ADMIN || userRole == UserRole.SUPER_ADMIN
    }

    fun isSuperAdmin(userId: UUID): Boolean = transaction {
        UsersTable.selectAll()
            .where { UsersTable.id eq userId }
            .singleOrNull()
            ?.get(UsersTable.role) == UserRole.SUPER_ADMIN
    }

    fun updateRole(targetUserId: UUID, newRole: UserRole): Boolean = transaction {
        UsersTable.update({ UsersTable.id eq targetUserId }) {
            it[role] = newRole
        } > 0
    }

    fun getUser(userId: UUID): User? = transaction {
        UsersTable.selectAll()
            .where { UsersTable.id eq userId }
            .singleOrNull()
            ?.let { row ->
                User(
                    id = row[UsersTable.id].value,
                    email = row[UsersTable.email],
                    role = row[UsersTable.role],
                )
            }
    }

    fun getAllUsers(): List<User> = transaction {
        UsersTable.selectAll().map { row ->
            User(
                id = row[UsersTable.id].value,
                email = row[UsersTable.email],
                role = row[UsersTable.role],
            )
        }
    }

    fun hasUsers(): Boolean = transaction {
        UsersTable.selectAll().count() > 0
    }
}
