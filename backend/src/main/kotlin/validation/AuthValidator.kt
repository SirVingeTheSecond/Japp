package com.japp.validation

import com.japp.models.AuthError
import com.japp.models.Result
import com.japp.models.dto.LoginRequest
import com.japp.models.dto.SignupRequest

object AuthValidator {

    fun validateSignup(request: SignupRequest): Result<SignupRequest, AuthError> {
        val errorFactory: (String) -> AuthError = { AuthError.ValidationError(it) }

        // Validate email
        ValidationHelpers.validateNotBlank(request.email, "Email", errorFactory)?.let {
            return Result.Failure(it.errorOrNull()!!)
        }
        ValidationHelpers.validateEmail(request.email, errorFactory)?.let {
            return Result.Failure(it.errorOrNull()!!)
        }

        // Validate username
        ValidationHelpers.validateNotBlank(request.username, "Username", errorFactory)?.let {
            return Result.Failure(it.errorOrNull()!!)
        }
        ValidationHelpers.validateLength(
            request.username,
            "Username",
            ValidationConstants.Length.USERNAME_MIN,
            ValidationConstants.Length.USERNAME_MAX,
            errorFactory
        )?.let {
            return Result.Failure(it.errorOrNull()!!)
        }
        ValidationHelpers.validateUsername(request.username, errorFactory)?.let {
            return Result.Failure(it.errorOrNull()!!)
        }

        // Validate firstname
        ValidationHelpers.validateNotBlank(request.firstname, "First name", errorFactory)?.let {
            return Result.Failure(it.errorOrNull()!!)
        }
        ValidationHelpers.validateLength(
            request.firstname,
            "First name",
            ValidationConstants.Length.NAME_MIN,
            ValidationConstants.Length.NAME_MAX,
            errorFactory
        )?.let {
            return Result.Failure(it.errorOrNull()!!)
        }

        // Validate lastname
        ValidationHelpers.validateNotBlank(request.lastname, "Last name", errorFactory)?.let {
            return Result.Failure(it.errorOrNull()!!)
        }
        ValidationHelpers.validateLength(
            request.lastname,
            "Last name",
            ValidationConstants.Length.NAME_MIN,
            ValidationConstants.Length.NAME_MAX,
            errorFactory
        )?.let {
            return Result.Failure(it.errorOrNull()!!)
        }

        // Validate password
        ValidationHelpers.validateNotBlank(request.password, "Password", errorFactory)?.let {
            return Result.Failure(it.errorOrNull()!!)
        }
        ValidationHelpers.validateLength(
            request.password,
            "Password",
            minLength = ValidationConstants.Length.PASSWORD_MIN,
            errorFactory = errorFactory
        )?.let {
            return Result.Failure(it.errorOrNull()!!)
        }
        ValidationHelpers.validatePassword(request.password, errorFactory)?.let {
            return Result.Failure(it.errorOrNull()!!)
        }

        // Validate phone (optional)
        ValidationHelpers.validateOptionalField(
            request.phone,
            "Phone",
            errorFactory = errorFactory
        )?.let {
            return Result.Failure(it.errorOrNull()!!)
        }

        return Result.Success(request)
    }

    fun validateLogin(request: LoginRequest): Result<LoginRequest, AuthError> {
        val errorFactory: (String) -> AuthError = { AuthError.ValidationError(it) }

        ValidationHelpers.validateNotBlank(
            request.emailOrUsername,
            "Email or username",
            errorFactory
        )?.let {
            return Result.Failure(it.errorOrNull()!!)
        }

        ValidationHelpers.validateNotBlank(
            request.password,
            "Password",
            errorFactory
        )?.let {
            return Result.Failure(it.errorOrNull()!!)
        }

        return Result.Success(request)
    }
}