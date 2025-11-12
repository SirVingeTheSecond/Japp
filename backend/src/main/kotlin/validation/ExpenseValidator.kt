package com.japp.validation

import com.japp.models.ExpenseError
import com.japp.models.Result
import com.japp.models.dto.CreateExpenseRequest

object ExpenseValidator {

    fun validateCreateExpense(request: CreateExpenseRequest): Result<CreateExpenseRequest, ExpenseError> {
        if (request.amount <= 0) {
            return Result.Failure(ExpenseError.ValidationError("Amount must be greater than 0"))
        }

        if (request.description.isBlank()) {
            return Result.Failure(ExpenseError.ValidationError("Description is required"))
        }

        if (request.description.length > 500) {
            return Result.Failure(ExpenseError.ValidationError("Description must not exceed 500 characters"))
        }

        // Example of needing to utilize Enums?
        if (request.splitType !in listOf("equal", "custom")) {
            return Result.Failure(ExpenseError.ValidationError("Split type must be 'equal' or 'custom'"))
        }

        if (request.splitType == "custom") {
            if (request.splits.isNullOrEmpty()) {
                return Result.Failure(ExpenseError.ValidationError("Custom split requires splits data"))
            }

            val totalSplitAmount = request.splits.sumOf { it.shareAmount ?: 0.0 }
            val totalSplitPercentage = request.splits.sumOf { it.sharePercentage ?: 0.0 }

            if (request.splits.all { it.shareAmount != null }) {
                if (kotlin.math.abs(totalSplitAmount - request.amount) > 0.01) {
                    return Result.Failure(
                        ExpenseError.ValidationError("Split amounts must sum to total expense amount")
                    )
                }
            }

            if (request.splits.all { it.sharePercentage != null }) {
                if (kotlin.math.abs(totalSplitPercentage - 100.0) > 0.01) {
                    return Result.Failure(
                        ExpenseError.ValidationError("Split percentages must sum to 100%")
                    )
                }
            }
        }

        return Result.Success(request)
    }
}