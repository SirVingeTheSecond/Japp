package com.japp.models.dto

import kotlinx.serialization.Serializable

@Serializable
data class FcmTokenRequest(
    val token: String
)
