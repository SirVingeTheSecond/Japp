package com.japp.services.interfaces

import com.japp.models.domain.User

/**
 * Repository interface for accessing user data
 */
interface IUserRepository {
    fun create(user: User): Int
    fun findById(id: Int): User?
    fun findByEmail(email: String): User?
    fun findByUsername(username: String): User?
    fun findByEmailOrUsername(identifier: String): User?
    fun findAll(): List<User>
    fun update(id: Int, user: User): Int
    fun delete(id: Int): Int
    fun emailExists(email: String): Boolean
    fun usernameExists(username: String): Boolean
    fun updateFcmToken(id: Int, token: String?): Int
    fun getFcmTokensForUsers(userIds: List<Int>): List<String>
    fun updateProfilePicture(id: Int, path: String?): Int
}
