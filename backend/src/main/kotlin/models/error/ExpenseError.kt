package com.japp.models.error


/**
 * Expense error providing a message and status code.
 */
sealed class ExpenseError(
    override val message: String,
    private val type: ErrorType
) : IAppError {

    override val httpStatus: Int get() = type.httpStatus

    class ValidationError(
        message: String
    ) : ExpenseError(message, ErrorType.VALIDATION)

    class NotFound(
        val expenseId: Int
    ) : ExpenseError("Expense not found", ErrorType.NOT_FOUND)

    class NotMember(
        val groupId: Int
    ) : ExpenseError("Not a member of this group", ErrorType.FORBIDDEN)

    class Unauthorized(
        message: String = "You do not have permission to perform this action"
    ) : ExpenseError(message, ErrorType.FORBIDDEN)

    class InternalError(
        message: String = "An unexpected error occurred"
    ) : ExpenseError(message, ErrorType.INTERNAL)
}