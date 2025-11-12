package com.japp.validation

import com.japp.models.Result
import com.japp.models.SettlementError
import com.japp.models.dto.CreateSettlementRequest

object SettlementValidator {

    fun validateCreateSettlement(request: CreateSettlementRequest): Result<CreateSettlementRequest, SettlementError> {
        if (request.amount <= 0) {
            return Result.Failure(SettlementError.ValidationError("Amount must be greater than 0"))
        }

        if (request.groupId <= 0) {
            return Result.Failure(SettlementError.ValidationError("Invalid group ID"))
        }

        if (request.toUserId <= 0) {
            return Result.Failure(SettlementError.ValidationError("Invalid recipient user ID"))
        }

        return Result.Success(request)
    }
}