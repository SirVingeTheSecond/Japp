package com.japp.api.responses

data class HealthResponse(
    val status: String,
    val version: String,
    val timestamp: Long,
)