package com.japp.models.error

/**
 * Unified application error describing all domain errors.
 */
// Resource type could be an Enum but I digress ¯\_(ツ)_/¯
sealed class AppError(
    override val message: String,
    private val type: ErrorType
) : IAppError {

    override val httpStatus: Int get() = type.httpStatus

    // ==============================
    // COMMON ERRORS
    // ==============================

    /**
     * Request validation failed (non-valid input, missing fields, constraint violations).
     */
    class Validation(
        message: String
    ) : AppError(message, ErrorType.VALIDATION)

    /**
     * Unexpected server error (database failures, unhandled exceptions).
     */
    class Internal(
        message: String = "An unexpected error occurred"
    ) : AppError(message, ErrorType.INTERNAL)

    /**
     * User is not a member of the specified group.
     */
    class NotMember(
        val groupId: Int
    ) : AppError("Not a member of this group", ErrorType.FORBIDDEN)

    /**
     * User lacks permission to perform the action.
     */
    class Unauthorized(
        message: String = "You do not have permission to perform this action"
    ) : AppError(message, ErrorType.FORBIDDEN)

    /**
     * Requested resource does not exist.
     *
     * @param resourceType Understandable resource name (could be "User", "Group", "Expense")
     * @param id The ID that was not found
     */
    class NotFound(
        resourceType: String,
        id: Int
    ) : AppError("$resourceType with $id was not found", ErrorType.NOT_FOUND)

    // ==============================
    // AUTH ERRORS
    // ==============================

    /**
     * Email address already registered in the system.
     */
    class EmailAlreadyExists(
        val email: String
    ) : AppError("Email $email already registered", ErrorType.CONFLICT)

    /**
     * Login credentials (email/username and password) are incorrect.
     */
    class InvalidCredentials : AppError(
        "Invalid email or password",
        ErrorType.UNAUTHORIZED
    )

    // ==============================
    // GROUP ERRORS
    // ==============================

    /**
     * Group invite code is invalid or expired.
     */
    class InvalidInviteCode : AppError(
        "Invalid invite code",
        ErrorType.NOT_FOUND
    )

    /**
     * User is already a member of the group.
     */
    class AlreadyMember : AppError(
        "Already a member of this group",
        ErrorType.CONFLICT
    )

    /**
     * Action requires group ownership (only group creator can perform).
     */
    class NotOwner(
        val groupId: Int
    ) : AppError(
        "Only the group owner can perform this action",
        ErrorType.FORBIDDEN
    )
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