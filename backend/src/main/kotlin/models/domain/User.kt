package com.japp.models.domain

data class User(
    val id: Int,
    val name: String,
    val email: String,
    val passwordHash: String,
    val phone: String?,
    val profilePicture: String?,
    val createdAt: String
)