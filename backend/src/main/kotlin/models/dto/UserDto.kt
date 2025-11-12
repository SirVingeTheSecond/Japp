package com.japp.models.dto

import kotlinx.serialization.Serializable

@Serializable
data class MeResponse(
    val userId: Int,
    val message: String
)