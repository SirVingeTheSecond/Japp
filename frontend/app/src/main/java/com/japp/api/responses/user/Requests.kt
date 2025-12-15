package com.japp.api.responses.user

data class FcmTokenRequest(val token: String)

data class MeResponse(
    val userId: Int,
    val message: String
)

data class UpdateUserRequest(
    val firstname: String? = null,
    val lastname: String? = null,
    val phone: String? = null,
    val profilePicture: String? = null
)
