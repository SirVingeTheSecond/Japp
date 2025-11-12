package com.example.japp.api.responses

import com.google.gson.annotations.SerializedName

data class HealthResponse(
    val status: String,
    val version: String,
    val timestamp: Long,
)