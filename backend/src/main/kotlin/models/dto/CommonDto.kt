package com.japp.models.dto

import kotlinx.serialization.Serializable

/**
 * Standard error response for all API errors
 */
@Serializable
data class ErrorResponse(
    val error: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Success response for operations that do not return data
 */
@Serializable
data class SuccessResponse(
    val success: Boolean = true,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Health check response
 */
@Serializable
data class HealthResponse(
    val status: String,
    val version: String,
    val timestamp: Long = System.currentTimeMillis()
)