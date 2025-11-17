package com.japp.models.error

/**
 * User profile error providing a message and status code.
 */
sealed class UserError(
    override val message: String,
    private val type: ErrorType
) : IAppError {

    override val httpStatus: Int get() = type.httpStatus

    class ValidationError(
        message: String
    ) : UserError(message, ErrorType.VALIDATION)

    class NotFound(
        val userId: Int
    ) : UserError("User not found", ErrorType.NOT_FOUND)

    class Unauthorized(
        message: String = "Cannot update another user's profile"
    ) : UserError(message, ErrorType.FORBIDDEN)

    class InternalError(
        message: String = "An unexpected error occurred"
    ) : UserError(message, ErrorType.INTERNAL)
}