package com.japp.models.dto

import kotlinx.serialization.Serializable

@Serializable
data class SignupRequest(
    val email: String,
    val username: String,
    val firstname: String,
    val lastname: String,
    val password: String,
    val phone: String? = null
)

@Serializable
data class LoginRequest(
    val emailOrUsername: String,
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
    val email: String,
    val username: String,
    val firstname: String,
    val lastname: String,
    val phone: String?,
    val profilePicture: String?
)