package com.example.japp.api.responses.auth

data class UserDto(
    val id: Int,
    val name: String,
    val email: String,
    val phone: String?,
    val profilePicture: String?
)