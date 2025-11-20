package com.japp.models.error

/**
 * Activity error providing a message and status code.
 */
sealed class ActivityError(
    override val message: String,
    private val type: ErrorType
) : IAppError {

    override val httpStatus: Int get() = type.httpStatus

    class NotMember(
        val groupId: Int
    ) : ActivityError("Not a member of this group", ErrorType.FORBIDDEN)

    class InternalError(
        message: String = "An unexpected error occurred"
    ) : ActivityError(message, ErrorType.INTERNAL)
}