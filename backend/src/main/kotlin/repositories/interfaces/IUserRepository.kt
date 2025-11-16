package com.japp.repositories.interfaces

import com.japp.models.domain.User

/**
 * Repository interface for accessing user data
 */
interface IUserRepository {
    fun create(user: User): Int
    fun findById(id: Int): User?
    fun findByEmail(email: String): User?
    fun findByUsername(username: String): User?
    fun findByEmailOrUsername(identifier: String): User? // Is this needed?
    fun findAll(): List<User>
    fun update(id: Int, user: User): Int
    fun delete(id: Int): Int
    fun emailExists(email: String): Boolean
    fun usernameExists(username: String): Boolean
}