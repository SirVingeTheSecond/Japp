package com.japp.models.error

/**
 * Attachment error providing a message and status code.
 */
sealed class AttachmentError(
    override val message: String,
    private val type: ErrorType
) : IAppError {

    override val httpStatus: Int get() = type.httpStatus

    class ValidationError(
        message: String
    ) : AttachmentError(message, ErrorType.VALIDATION)

    class NotFound(
        val attachmentId: Int
    ) : AttachmentError("Attachment not found", ErrorType.NOT_FOUND)

    class ExpenseNotFound(
        val expenseId: Int
    ) : AttachmentError("Expense not found", ErrorType.NOT_FOUND)

    class NotMember(
        val groupId: Int
    ) : AttachmentError("Not a member of this group", ErrorType.FORBIDDEN)

    class Unauthorized(
        message: String = "You do not have permission to perform this action"
    ) : AttachmentError(message, ErrorType.FORBIDDEN)

    class InternalError(
        message: String = "An unexpected error occurred"
    ) : AttachmentError(message, ErrorType.INTERNAL)
}