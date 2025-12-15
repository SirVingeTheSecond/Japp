package com.japp.repositories.implementations

import com.japp.database.tables.Users
import com.japp.models.domain.User
import com.japp.services.interfaces.IUserRepository
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.*

class UserRepository : IUserRepository {

    override fun create(user: User): Int {
        return Users.insert {
            it[email] = user.email
            it[username] = user.username
            it[firstname] = user.firstname
            it[lastname] = user.lastname
            it[passwordHash] = user.passwordHash
            it[phone] = user.phone
            it[profilePicture] = user.profilePicture
            it[fcmToken] = user.fcmToken
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
        return Users.selectAll()
            .map { rowToUser(it) }
    }

    override fun update(id: Int, user: User): Int {
        return Users.update({ Users.id eq id }) {
            it[firstname] = user.firstname
            it[lastname] = user.lastname
            it[phone] = user.phone
            it[profilePicture] = user.profilePicture
            it[fcmToken] = user.fcmToken
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

    override fun updateFcmToken(id: Int, token: String?): Int {
        return Users.update({ Users.id eq id }) {
            it[fcmToken] = token
        }
    }

    // For sending notifications to multiple users
    override fun getFcmTokensForUsers(userIds: List<Int>): List<String> {
        if (userIds.isEmpty()) return emptyList()

        return Users.selectAll()
            .where { Users.id inList userIds }
            .mapNotNull { it[Users.fcmToken] }
            .filter { it.isNotBlank() }
    }

    override fun updateProfilePicture(id: Int, path: String?): Int {
        return Users.update({ Users.id eq id }) {
            it[profilePicture] = path
        }
    }

    private fun rowToUser(row: ResultRow): User {
        return User(
            id = row[Users.id],
            email = row[Users.email],
            username = row[Users.username],
            firstname = row[Users.firstname],
            lastname = row[Users.lastname],
            passwordHash = row[Users.passwordHash],
            phone = row[Users.phone],
            profilePicture = row[Users.profilePicture],
            fcmToken = row[Users.fcmToken],
            createdAt = row[Users.createdAt]
        )
    }
}
