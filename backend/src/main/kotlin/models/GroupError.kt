package com.japp.models

sealed class GroupError {
    abstract val message: String
    abstract val httpStatus: Int

    data class ValidationError(
        override val message: String,
        override val httpStatus: Int = 400
    ) : GroupError()

    data class NotFound(
        val groupId: Int,
        override val message: String = "Group not found",
        override val httpStatus: Int = 404
    ) : GroupError()

    data class InvalidInviteCode(
        override val message: String = "Invalid or expired invite code",
        override val httpStatus: Int = 404
    ) : GroupError()

    data class AlreadyMember(
        val groupId: Int,
        override val message: String = "Already a member of this group",
        override val httpStatus: Int = 409
    ) : GroupError()

    data class NotMember(
        val groupId: Int,
        override val message: String = "Not a member of this group",
        override val httpStatus: Int = 403
    ) : GroupError()

    data class NotOwner(
        val groupId: Int,
        override val message: String = "Only group owner can perform this action",
        override val httpStatus: Int = 403
    ) : GroupError()

    data class InternalError(
        override val message: String = "An unexpected error occurred",
        override val httpStatus: Int = 500
    ) : GroupError()
}