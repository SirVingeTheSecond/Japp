package com.example.japp.api.responses.auth

data class SignupRequest(
    val name: String,
    val email: String,
    val password: String,
    val phone: String? = null
)

data class LoginRequest(
    val email: String,
    val password: String
)


// Responses
data class AuthResponse(
    val token: String,
    val user: UserDto
)