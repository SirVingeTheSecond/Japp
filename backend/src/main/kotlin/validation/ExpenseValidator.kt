package com.japp.validation

import com.japp.models.Result
import com.japp.models.SplitType
import com.japp.models.dto.CreateExpenseRequest
import com.japp.models.error.AppError
import com.japp.utils.ValidationHelpers
import kotlin.math.abs

object ExpenseValidator {

    fun validateCreateExpense(request: CreateExpenseRequest): Result<CreateExpenseRequest, AppError> {
        val errorFactory: (String) -> AppError = { AppError.Validation(it) }

        // amount
        ValidationHelpers.validatePositiveAmount(
            request.amount,
            "Amount",
            errorFactory
        )?.let {
            return Result.Failure(it)
        }

        // description
        ValidationHelpers.validateNotBlank(
            request.description,
            "Description",
            errorFactory
        )?.let {
            return Result.Failure(it)
        }

        ValidationHelpers.validateLength(
            request.description,
            "Description",
            maxLength = ValidationConstants.Length.EXPENSE_DESCRIPTION_MAX,
            errorFactory = errorFactory
        )?.let {
            return Result.Failure(it)
        }

        // custom split
        if (request.splitType == SplitType.CUSTOM) {
            if (request.splits.isNullOrEmpty()) {
                return Result.Failure(
                    AppError.Validation(ValidationConstants.Messages.CUSTOM_SPLIT_NEEDS_DATA)
                )
            }

            val totalSplitAmount = request.splits.sumOf { it.shareAmount ?: 0.0 }
            val totalSplitPercentage = request.splits.sumOf { it.sharePercentage ?: 0.0 }

            // amount splits
            if (request.splits.all { it.shareAmount != null }) {
                if (abs(totalSplitAmount - request.amount) > ValidationConstants.Amount.COMPARISON_TOLERANCE) {
                    return Result.Failure(
                        AppError.Validation(ValidationConstants.Messages.SPLIT_AMOUNTS_MISMATCH)
                    )
                }
            }

            // percentage splits
            if (request.splits.all { it.sharePercentage != null }) {
                if (abs(totalSplitPercentage - ValidationConstants.Amount.PERCENTAGE_TOTAL) > ValidationConstants.Amount.COMPARISON_TOLERANCE) {
                    return Result.Failure(
                        AppError.Validation(ValidationConstants.Messages.SPLIT_PERCENTAGES_MISMATCH)
                    )
                }
            }
        }

        return Result.Success(request)
    }
}