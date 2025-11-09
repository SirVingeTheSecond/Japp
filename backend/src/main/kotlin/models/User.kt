package com.japp.models

import kotlinx.serialization.Serializable

data class User(
    val id: Int,
    val name: String,
    val email: String,
    val passwordHash: String,
    val phone: String?,
    val profilePicture: String?,
    val createdAt: String
)

/**
 * DTOs for API requests/responses
 */

@Serializable
data class SignupRequest(
    val email: String,
    val password: String,
    val name: String,
    val phone: String? = null
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class AuthResponse(
    val token: String,
    val user: UserDto
)

@Serializable
data class UserDto(
    val id: Int,
    val name: String,
    val email: String,
    val phone: String? = null,
    val profilePicture: String? = null
)