package com.japp.repositories.implementation

import com.japp.database.tables.Users
import com.japp.models.domain.User
import com.japp.repositories.interfaces.IUserRepository
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.*

/**
 * Handles all database operations for users
 */
class UserRepository : IUserRepository {

    override fun create(user: User): Int {
        return Users.insert {
            it[email] = user.email
            it[username] = user.username
            it[firstName] = user.firstname
            it[lastName] = user.lastname
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

    override fun findByUsername(username: String): User? {
        return Users.selectAll()
            .where { Users.username eq username }
            .map { rowToUser(it) }
            .singleOrNull()
    }

    override fun findByEmailOrUsername(identifier: String): User? {
        return Users.selectAll()
            .where { (Users.email eq identifier) or (Users.username eq identifier) }
            .map { rowToUser(it) }
            .singleOrNull()
    }

    override fun findAll(): List<User> {
        return Users.selectAll().map { rowToUser(it) }
    }

    override fun update(id: Int, user: User): Int {
        return Users.update({ Users.id eq id }) {
            it[email] = user.email
            it[username] = user.username
            it[firstName] = user.firstname
            it[lastName] = user.lastname
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

    override fun usernameExists(username: String): Boolean {
        return Users.selectAll()
            .where { Users.username eq username }
            .count() > 0
    }

    private fun rowToUser(row: ResultRow) = User(
        id = row[Users.id],
        email = row[Users.email],
        username = row[Users.username],
        firstname = row[Users.firstName],
        lastname = row[Users.lastName],
        passwordHash = row[Users.passwordHash],
        phone = row[Users.phone],
        profilePicture = row[Users.profilePicture],
        createdAt = row[Users.createdAt]
    )
}