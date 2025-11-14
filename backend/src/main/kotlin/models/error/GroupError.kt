package com.japp.models.error

/**
 * Group error providing a message and status code.
 */
sealed class GroupError(
    override val message: String,
    private val type: ErrorType
) : IAppError {

    override val httpStatus: Int get() = type.httpStatus

    class ValidationError(
        message: String
    ) : GroupError(message, ErrorType.VALIDATION)

    class NotFound(
        val groupId: Int
    ) : GroupError("Group not found", ErrorType.NOT_FOUND)

    class NotMember(
        val groupId: Int
    ) : GroupError("Not a member of this group", ErrorType.FORBIDDEN)

    class NotOwner(
        val groupId: Int
    ) : GroupError("Only the group owner can perform this action", ErrorType.FORBIDDEN)

    class InvalidInviteCode : GroupError(
        "Invalid invite code",
        ErrorType.NOT_FOUND
    )

    class AlreadyMember : GroupError(
        "Already a member of this group",
        ErrorType.CONFLICT
    )

    class InternalError(
        message: String = "An unexpected error occurred"
    ) : GroupError(message, ErrorType.INTERNAL)
}