package com.japp.api.responses.auth

data class SignupRequest(
    val email: String,
    val username: String,
    val firstname: String,
    val lastname: String,
    val password: String,
    val phone: String? = null
)

data class LoginRequest(
    val emailOrUsername: String,
    val password: String
)


// Responses
data class AuthResponse(
    val token: String,
    val user: UserDto
)