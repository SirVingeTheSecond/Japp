package com.japp.validation

import com.japp.models.GroupError
import com.japp.models.Result
import com.japp.models.dto.CreateGroupRequest
import com.japp.models.dto.JoinGroupRequest

object GroupValidator {

    fun validateCreateGroup(request: CreateGroupRequest): Result<CreateGroupRequest, GroupError> {
        if (request.name.isBlank()) {
            return Result.Failure(GroupError.ValidationError("Group name is required"))
        }
        if (request.name.length < 2) {
            return Result.Failure(GroupError.ValidationError("Group name must be at least 2 characters"))
        }
        if (request.name.length > 100) {
            return Result.Failure(GroupError.ValidationError("Group name must not exceed 100 characters"))
        }

        return Result.Success(request)
    }

    fun validateJoinGroup(request: JoinGroupRequest): Result<JoinGroupRequest, GroupError> {
        if (request.inviteCode.isBlank()) {
            return Result.Failure(GroupError.ValidationError("Invite code is required"))
        }
        if (request.inviteCode.length != 6) {
            return Result.Failure(GroupError.ValidationError("Invalid invite code"))
        }

        return Result.Success(request)
    }
}