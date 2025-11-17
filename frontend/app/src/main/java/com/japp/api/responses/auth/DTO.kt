package com.japp.api.responses.auth

data class UserDto(
    val id: Int,
    val email: String,
    val username: String,
    val firstname: String,
    val lastname: String,
    val phone: String?,
    val profilePicture: String?
)