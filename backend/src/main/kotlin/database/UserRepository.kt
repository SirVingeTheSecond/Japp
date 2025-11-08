package com.japp.database

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.v1.jdbc.insert

import com.japp.models.User
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction

object Users : Table() {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 255)
    val email = varchar("email", 255)
    val password_hash = varchar("passwordHash", 255)
    val created_at = varchar("createdAt", 255)
    val profile_picture = varchar("profilePicture", 700).nullable()
    val phone = varchar("phone", 255).nullable()

    override val primaryKey = PrimaryKey(id)
}

class UserService(private val database: Database) {

    init {
        transaction(database) {
            SchemaUtils.create(Users)
        }
    }

    suspend fun create(user: User): Int = dbQuery {
        Users.insert {
            it[name] = user.name
            it[email] = user.email
            it[password_hash] = user.passwordHash
            it[created_at] = user.createdAt
            it[profile_picture] = user.profilePicture
            it[phone] = user.phone
        }[Users.id]
    }

    suspend fun read(id: Int): User = dbQuery {
        Users.selectAll().where { Users.id eq id }
            .map { rowToUser(it) }
            .singleOrNull() ?: throw Exception("User not found")
    }

    private fun rowToUser(row: ResultRow) = User(
        id = row[Users.id],
        name = row[Users.name],
        email = row[Users.email],
        passwordHash = row[Users.password_hash],
        createdAt = row[Users.created_at],
        profilePicture = row[Users.profile_picture],
        phone = row[Users.phone],
    )

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO, database) { block() }
}