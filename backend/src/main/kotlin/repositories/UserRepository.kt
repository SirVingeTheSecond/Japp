package com.japp.repositories

import com.japp.database.tables.Users
import com.japp.models.domain.User
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.*

/**
 * Handles all database operations for users
 */
class UserRepository : IUserRepository {

    override fun create(user: User): Int {
        return Users.insert {
            it[name] = user.name
            it[email] = user.email
            it[passwordHash] = user.passwordHash
            it[phone] = user.phone
            it[profilePicture] = user.profilePicture
            it[createdAt] = user.createdAt
        }[Users.id]
    }

    override fun findById(id: Int): User? {
        return Users.selectAll()
            .where { Users.id eq id }
            .map { rowToUser(it) }
            .singleOrNull()
    }

    override fun findByEmail(email: String): User? {
        return Users.selectAll()
            .where { Users.email eq email }
            .map { rowToUser(it) }
            .singleOrNull()
    }

    override fun findAll(): List<User> {
        return Users.selectAll().map { rowToUser(it) }
    }

    override fun update(id: Int, user: User): Int {
        return Users.update({ Users.id eq id }) {
            it[name] = user.name
            it[email] = user.email
            it[phone] = user.phone
            it[profilePicture] = user.profilePicture
        }
    }

    override fun delete(id: Int): Int {
        return Users.deleteWhere { Users.id eq id }
    }

    override fun emailExists(email: String): Boolean {
        return Users.selectAll()
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
}