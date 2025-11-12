package com.japp.models

/**
 * Authentication error providing a message and status code.
 */
sealed class AuthError(
    override val message: String,
    private val type: ErrorType
) : IAppError {

    override val httpStatus: Int get() = type.httpStatus

    class ValidationError(
        message: String
    ) : AuthError(message, ErrorType.VALIDATION)

    class EmailAlreadyExists(
        email: String
    ) : AuthError("Email $email already registered", ErrorType.CONFLICT)

    class InvalidCredentials : AuthError(
        "Invalid email or password",
        ErrorType.UNAUTHORIZED
    )

    class UserNotFound : AuthError(
        "User not found",
        ErrorType.NOT_FOUND
    )

    class InternalError(
        message: String = "An unexpected error occurred"
    ) : AuthError(message, ErrorType.INTERNAL)
}