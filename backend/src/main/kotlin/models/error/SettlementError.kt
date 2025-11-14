package com.japp.models.error

/**
 * Settlement error providing a message and status code.
 */
sealed class SettlementError(
    override val message: String,
    private val type: ErrorType
) : IAppError {

    override val httpStatus: Int get() = type.httpStatus

    class ValidationError(
        message: String
    ) : SettlementError(message, ErrorType.VALIDATION)

    class NotFound(
        val settlementId: Int
    ) : SettlementError("Settlement not found", ErrorType.NOT_FOUND)

    class NotMember(
        val groupId: Int
    ) : SettlementError("Not a member of this group", ErrorType.FORBIDDEN)

    class Unauthorized(
        message: String = "You do not have permission to perform this action"
    ) : SettlementError(message, ErrorType.FORBIDDEN)

    class InternalError(
        message: String = "An unexpected error occurred"
    ) : SettlementError(message, ErrorType.INTERNAL)
}