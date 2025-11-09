package com.japp.repositories

import com.japp.models.User
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

object Users : Table("users") {
    val id = integer("id").autoIncrement() // ToDo: Replace with UUID?
    val name = varchar("name", 255)
    val email = varchar("email", 255).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val phone = varchar("phone", 255).nullable()
    val profilePicture = varchar("profile_picture", 700).nullable()
    val createdAt = varchar("created_at", 255)

    override val primaryKey = PrimaryKey(id)
}

/**
 * Handles all database operations for users
 */
class UserRepository() {

    init {
        transaction {
            SchemaUtils.create(Users)
        }
    }

    suspend fun create(user: User): Int = dbQuery {
        Users.insert {
            it[name] = user.name
            it[email] = user.email
            it[passwordHash] = user.passwordHash
            it[phone] = user.phone
            it[profilePicture] = user.profilePicture
            it[createdAt] = user.createdAt
        }[Users.id]
    }

    suspend fun findById(id: Int): User? = dbQuery {
        Users.selectAll()
            .where { Users.id eq id }
            .map { rowToUser(it) }
            .singleOrNull()
    }

    suspend fun findByEmail(email: String): User? = dbQuery {
        Users.selectAll()
            .where { Users.email eq email }
            .map { rowToUser(it) }
            .singleOrNull()
    }

    suspend fun findAll(): List<User> = dbQuery {
        Users.selectAll().map { rowToUser(it) }
    }

    suspend fun update(id: Int, user: User): Int = dbQuery {
        Users.update({ Users.id eq id }) {
            it[name] = user.name
            it[email] = user.email
            it[phone] = user.phone
            it[profilePicture] = user.profilePicture
        }
    }

    suspend fun delete(id: Int): Int = dbQuery {
        Users.deleteWhere { Users.id eq id }
    }

    suspend fun emailExists(email: String): Boolean = dbQuery {
        Users.selectAll()
            .where { Users.email eq email }
            .count() > 0
    }

    private fun rowToUser(row: ResultRow) = User(
        id = row[Users.id],
        name = row[Users.name],
        email = row[Users.email],
        passwordHash = row[Users.passwordHash],
        phone = row[Users.phone],
        profilePicture = row[Users.profilePicture],
        createdAt = row[Users.createdAt]
    )

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}