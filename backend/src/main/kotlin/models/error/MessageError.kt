package com.japp.models.error

/**
 * Message error providing a message and status code.
 */
sealed class MessageError(
    override val message: String,
    private val type: ErrorType
) : IAppError {

    override val httpStatus: Int get() = type.httpStatus

    class ValidationError(
        message: String
    ) : MessageError(message, ErrorType.VALIDATION)

    class NotFound(
        val messageId: Int
    ) : MessageError("Message not found", ErrorType.NOT_FOUND)

    class NotMember(
        val groupId: Int
    ) : MessageError("Not a member of this group", ErrorType.FORBIDDEN)

    class Unauthorized(
        message: String = "You do not have permission to perform this action"
    ) : MessageError(message, ErrorType.FORBIDDEN)

    class InternalError(
        message: String = "An unexpected error occurred"
    ) : MessageError(message, ErrorType.INTERNAL)
}