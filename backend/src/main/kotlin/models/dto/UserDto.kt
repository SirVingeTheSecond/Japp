package com.japp.models.dto

import kotlinx.serialization.Serializable

@Serializable
data class MeResponse(
    val userId: Int,
    val message: String
)

@Serializable
data class UpdateUserRequest(
    val firstname: String? = null,
    val lastname: String? = null,
    val phone: String? = null,
    val profilePicture: String? = null
)