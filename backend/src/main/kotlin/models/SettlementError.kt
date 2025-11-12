package com.japp.models

sealed class SettlementError {
    abstract val message: String
    abstract val httpStatus: Int

    data class ValidationError(
        override val message: String,
        override val httpStatus: Int = 400
    ) : SettlementError()

    data class NotFound(
        val settlementId: Int,
        override val message: String = "Settlement not found",
        override val httpStatus: Int = 404
    ) : SettlementError()

    data class NotMember(
        val groupId: Int,
        override val message: String = "Not a member of this group",
        override val httpStatus: Int = 403
    ) : SettlementError()

    data class Unauthorized(
        override val message: String = "You do not have permission to perform this action",
        override val httpStatus: Int = 403
    ) : SettlementError()

    data class InternalError(
        override val message: String = "An unexpected error occurred",
        override val httpStatus: Int = 500
    ) : SettlementError()
}