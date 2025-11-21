package com.japp.validation

import com.japp.models.Result
import com.japp.models.dto.CreateSettlementRequest
import com.japp.models.error.AppError
import com.japp.utils.ValidationHelpers

object SettlementValidator {

    fun validateCreateSettlement(request: CreateSettlementRequest): Result<CreateSettlementRequest, AppError> {
        val errorFactory: (String) -> AppError = { AppError.Validation(it) }

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
            return Result.Failure(AppError.Validation("Invalid group ID"))
        }

        // Validate recipient user ID
        if (request.toUserId <= 0) {
            return Result.Failure(AppError.Validation("Invalid recipient user ID"))
        }

        return Result.Success(request)
    }
}