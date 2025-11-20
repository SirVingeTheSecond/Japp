package com.japp.validation

import com.japp.models.Result
import com.japp.models.dto.CreateMessageRequest
import com.japp.models.error.AppError

object MessageValidator {

    private const val MAX_MESSAGE_LENGTH = 2000

    fun validateCreateMessage(request: CreateMessageRequest): Result<CreateMessageRequest, AppError> {
        val errorFactory: (String) -> AppError = { AppError.Validation(it) }

        ValidationHelpers.validateNotBlank(
            request.content,
            "Message content",
            errorFactory
        )?.let {
            return Result.Failure(it)
        }

        if (request.content.length > MAX_MESSAGE_LENGTH) {
            return Result.Failure(
                AppError.Validation("Message cannot exceed $MAX_MESSAGE_LENGTH characters")
            )
        }

        if (request.groupId <= 0) {
            return Result.Failure(AppError.Validation("Invalid group ID"))
        }

        return Result.Success(request)
    }
}