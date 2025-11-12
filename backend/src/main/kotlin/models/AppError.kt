package com.japp.models

sealed interface IAppError {
    val message: String
    val httpStatus: Int
}

/**
 * HTTP error types with status codes
 */
enum class ErrorType(val httpStatus: Int) {
    VALIDATION(400),
    UNAUTHORIZED(401),
    FORBIDDEN(403),
    NOT_FOUND(404),
    CONFLICT(409),
    INTERNAL(500)
}
