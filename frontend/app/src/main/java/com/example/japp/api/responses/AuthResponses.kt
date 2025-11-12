package com.example.japp.api.responses

class AuthResponses {
    // Requests
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

    data class UserDto(
        val id: Int,
        val name: String,
        val email: String,
        val phone: String?,
        val profilePicture: String?
    )
}
