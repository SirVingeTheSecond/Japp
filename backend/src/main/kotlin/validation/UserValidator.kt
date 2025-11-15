package com.japp.validation

import com.japp.models.Result
import com.japp.models.UserError
import com.japp.models.dto.UpdateUserRequest

object UserValidator {

    fun validateUpdateProfile(request: UpdateUserRequest): Result<UpdateUserRequest, UserError> {
        // Validate firstname if provided
        request.firstname?.let { firstname ->
            if (firstname.isBlank()) {
                return Result.Failure(UserError.ValidationError("First name cannot be blank"))
            }
            if (firstname.length < 2) {
                return Result.Failure(UserError.ValidationError("First name must be at least 2 characters"))
            }
            if (firstname.length > 100) {
                return Result.Failure(UserError.ValidationError("First name must not exceed 100 characters"))
            }
        }

        // Validate lastname if provided
        request.lastname?.let { lastname ->
            if (lastname.isBlank()) {
                return Result.Failure(UserError.ValidationError("Last name cannot be blank"))
            }
            if (lastname.length < 2) {
                return Result.Failure(UserError.ValidationError("Last name must be at least 2 characters"))
            }
            if (lastname.length > 100) {
                return Result.Failure(UserError.ValidationError("Last name must not exceed 100 characters"))
            }
        }

        // Validate phone if provided
        request.phone?.let { phone ->
            if (phone.isBlank()) {
                return Result.Failure(UserError.ValidationError("Phone cannot be blank if provided"))
            }
        }

        // Validate profilePicture if provided
        request.profilePicture?.let { picture ->
            if (picture.isBlank()) {
                return Result.Failure(UserError.ValidationError("Profile picture URL cannot be blank if provided"))
            }
        }

        // At least one field must be provided
        if (request.firstname == null &&
            request.lastname == null &&
            request.phone == null &&
            request.profilePicture == null) {
            return Result.Failure(UserError.ValidationError("At least one field must be provided for update"))
        }

        return Result.Success(request)
    }
}