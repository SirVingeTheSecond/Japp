package com.japp.models.domain

data class User(
    val id: Int,
    val username: String,
    val firstname: String,
    val lastname: String,
    val email: String,
    val passwordHash: String,
    val phone: String?,
    val profilePicture: String?,
    val fcmToken: String?,
    val createdAt: String
)