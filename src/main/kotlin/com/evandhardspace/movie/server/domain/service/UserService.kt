package com.evandhardspace.movie.server.domain.service

import com.evandhardspace.movie.server.domain.table.UsersTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.upsert
import java.util.UUID

class UserService {
    fun upsertUser(userId: UUID, email: String) {
        transaction {
            UsersTable.upsert(onUpdateExclude = listOf(UsersTable.isAdmin, UsersTable.createdAt)) {
                it[id] = EntityID(userId, UsersTable)
                it[this.email] = email
                it[isAdmin] = false
            }
        }
    }
}
