package com.japp.validation

import com.japp.models.Result
import com.japp.models.dto.CreateMessageRequest
import com.japp.models.error.MessageError

object MessageValidator {

    private const val MAX_MESSAGE_LENGTH = 2000

    fun validateCreateMessage(request: CreateMessageRequest): Result<CreateMessageRequest, MessageError> {
        val errorFactory: (String) -> MessageError = { MessageError.ValidationError(it) }

        ValidationHelpers.validateNotBlank(
            request.content,
            "Message content",
            errorFactory
        )?.let {
            return Result.Failure(it.errorOrNull()!!)
        }

        if (request.content.length > MAX_MESSAGE_LENGTH) {
            return Result.Failure(
                MessageError.ValidationError("Message cannot exceed $MAX_MESSAGE_LENGTH characters")
            )
        }

        if (request.groupId <= 0) {
            return Result.Failure(MessageError.ValidationError("Invalid group ID"))
        }

        return Result.Success(request)
    }
}