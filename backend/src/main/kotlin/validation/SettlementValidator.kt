package com.japp.validation

import com.japp.models.Result
import com.japp.models.error.SettlementError
import com.japp.models.dto.CreateSettlementRequest

object SettlementValidator {

    fun validateCreateSettlement(request: CreateSettlementRequest): Result<CreateSettlementRequest, SettlementError> {
        val errorFactory: (String) -> SettlementError = { SettlementError.ValidationError(it) }

        // Validate amount
        ValidationHelpers.validatePositiveAmount(
            request.amount,
            "Amount",
            errorFactory
        )?.let {
            return Result.Failure(it)
        }

        // Validate group ID
        if (request.groupId <= 0) {
            return Result.Failure(SettlementError.ValidationError("Invalid group ID"))
        }

        // Validate recipient user ID
        if (request.toUserId <= 0) {
            return Result.Failure(SettlementError.ValidationError("Invalid recipient user ID"))
        }

        return Result.Success(request)
    }
}