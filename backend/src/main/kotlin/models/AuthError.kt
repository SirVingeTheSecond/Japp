package com.japp.models

/**
 * Authentication error providing a message and status code.
 */
sealed class AuthError {
    abstract val message: String
    abstract val httpStatus: Int

    data class ValidationError(
        override val message: String,
        override val httpStatus: Int = 400
    ) : AuthError()

    data class EmailAlreadyExists(
        val email: String,
        override val message: String = "Email already registered",
        override val httpStatus: Int = 409
    ) : AuthError()

    data class InvalidCredentials(
        override val message: String = "Invalid email or password",
        override val httpStatus: Int = 401
    ) : AuthError()

    data class UserNotFound(
        override val message: String = "User not found",
        override val httpStatus: Int = 404
    ) : AuthError()

    data class InternalError(
        override val message: String = "An unexpected error occurred",
        override val httpStatus: Int = 500
    ) : AuthError()
}