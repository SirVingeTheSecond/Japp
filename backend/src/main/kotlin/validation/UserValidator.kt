package com.japp.validation

import com.japp.models.Result
import com.japp.models.dto.UpdateUserRequest
import com.japp.models.error.AppError

object UserValidator {

    fun validateUpdateProfile(request: UpdateUserRequest): Result<UpdateUserRequest, AppError> {
        val errorFactory: (String) -> AppError = { AppError.Validation(it) }

        // Validate firstname if provided
        ValidationHelpers.validateOptionalField(
            request.firstname,
            "First name",
            ValidationConstants.Length.NAME_MIN,
            ValidationConstants.Length.NAME_MAX,
            errorFactory
        )?.let {
            return Result.Failure(it)
        }

        // Validate lastname if provided
        ValidationHelpers.validateOptionalField(
            request.lastname,
            "Last name",
            ValidationConstants.Length.NAME_MIN,
            ValidationConstants.Length.NAME_MAX,
            errorFactory
        )?.let {
            return Result.Failure(it)
        }

        // Validate phone if provided
        ValidationHelpers.validateOptionalField(
            request.phone,
            "Phone",
            errorFactory = errorFactory
        )?.let {
            return Result.Failure(it)
        }

        // Validate profilePicture if provided
        ValidationHelpers.validateOptionalField(
            request.profilePicture,
            "Profile picture URL",
            errorFactory = errorFactory
        )?.let {
            return Result.Failure(it)
        }

        // At least one field must be provided
        if (request.firstname == null &&
            request.lastname == null &&
            request.phone == null &&
            request.profilePicture == null) {
            return Result.Failure(
                AppError.Validation(ValidationConstants.Messages.AT_LEAST_ONE_FIELD)
            )
        }

        return Result.Success(request)
    }
}