package com.japp.validation

import com.japp.models.Result
import com.japp.models.dto.CreateGroupRequest
import com.japp.models.dto.JoinGroupRequest
import com.japp.models.error.AppError
import com.japp.utils.ValidationHelpers

object GroupValidator {

    fun validateCreateGroup(request: CreateGroupRequest): Result<CreateGroupRequest, AppError> {
        val errorFactory: (String) -> AppError = { AppError.Validation(it) }

        ValidationHelpers.validateNotBlank(request.name, "Group name", errorFactory)?.let {
            return Result.Failure(it)
        }
        ValidationHelpers.validateLength(
            request.name,
            "Group name",
            ValidationConstants.Length.GROUP_NAME_MIN,
            ValidationConstants.Length.GROUP_NAME_MAX,
            errorFactory
        )?.let {
            return Result.Failure(it)
        }

        return Result.Success(request)
    }

    fun validateJoinGroup(request: JoinGroupRequest): Result<JoinGroupRequest, AppError> {
        val errorFactory: (String) -> AppError = { AppError.Validation(it) }

        ValidationHelpers.validateNotBlank(
            request.inviteCode,
            "Invite code",
            errorFactory
        )?.let {
            return Result.Failure(it)
        }

        if (request.inviteCode.length != ValidationConstants.Length.INVITE_CODE_LENGTH) {
            return Result.Failure(
                AppError.Validation(ValidationConstants.Messages.INVALID_INVITE_CODE)
            )
        }

        return Result.Success(request)
    }

    fun validateAddMember(userId: Int): Result<Int, AppError> {
        if (userId <= 0) {
            return Result.Failure(
                AppError.Validation("Invalid user ID")
            )
        }

        return Result.Success(userId)
    }

    fun validatePreviewInviteCode(inviteCode: String): Result<String, AppError> {
        val errorFactory: (String) -> AppError = { AppError.Validation(it) }

        ValidationHelpers.validateNotBlank(
            inviteCode,
            "Invite code",
            errorFactory
        )?.let {
            return Result.Failure(it)
        }

        if (inviteCode.length != ValidationConstants.Length.INVITE_CODE_LENGTH) {
            return Result.Failure(
                AppError.Validation(ValidationConstants.Messages.INVALID_INVITE_CODE)
            )
        }

        return Result.Success(inviteCode)
    }
}