package com.japp.validation

import com.japp.models.GroupError
import com.japp.models.Result
import com.japp.models.dto.CreateGroupRequest
import com.japp.models.dto.JoinGroupRequest

object GroupValidator {

    fun validateCreateGroup(request: CreateGroupRequest): Result<CreateGroupRequest, GroupError> {
        val errorFactory: (String) -> GroupError = { GroupError.ValidationError(it) }

        ValidationHelpers.validateNotBlank(request.name, "Group name", errorFactory)?.let {
            return Result.Failure(it.errorOrNull()!!)
        }
        ValidationHelpers.validateLength(
            request.name,
            "Group name",
            ValidationConstants.Length.GROUP_NAME_MIN,
            ValidationConstants.Length.GROUP_NAME_MAX,
            errorFactory
        )?.let {
            return Result.Failure(it.errorOrNull()!!)
        }

        return Result.Success(request)
    }

    fun validateJoinGroup(request: JoinGroupRequest): Result<JoinGroupRequest, GroupError> {
        val errorFactory: (String) -> GroupError = { GroupError.ValidationError(it) }

        ValidationHelpers.validateNotBlank(
            request.inviteCode,
            "Invite code",
            errorFactory
        )?.let {
            return Result.Failure(it.errorOrNull()!!)
        }

        if (request.inviteCode.length != ValidationConstants.Length.INVITE_CODE_LENGTH) {
            return Result.Failure(
                GroupError.ValidationError(ValidationConstants.Messages.INVALID_INVITE_CODE)
            )
        }

        return Result.Success(request)
    }
}