package com.evandhardspace.movie.server.domain.service

import com.evandhardspace.movie.server.domain.model.UserRole
import com.evandhardspace.movie.server.domain.table.UsersTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.jdbc.upsert
import java.util.UUID

class UserService {
    fun upsertUser(userId: UUID, email: String) {
        transaction {
            UsersTable.upsert(onUpdateExclude = listOf(UsersTable.role, UsersTable.createdAt)) {
                it[id] = EntityID(userId, UsersTable)
                it[this.email] = email
                it[role] = UserRole.USER
            }
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
}
