package com.japp.validation

import com.japp.models.error.AuthError
import com.japp.models.Result
import com.japp.models.dto.LoginRequest
import com.japp.models.dto.SignupRequest

object AuthValidator {

    fun validateSignup(request: SignupRequest): Result<SignupRequest, AuthError> {
        val errorFactory: (String) -> AuthError = { AuthError.ValidationError(it) }

        // Validate email
        ValidationHelpers.validateNotBlank(request.email, "Email", errorFactory)?.let {
            return Result.Failure(it)
        }
        ValidationHelpers.validateEmail(request.email, errorFactory)?.let {
            return Result.Failure(it)
        }

        // Validate username
        ValidationHelpers.validateNotBlank(request.username, "Username", errorFactory)?.let {
            return Result.Failure(it)
        }
        ValidationHelpers.validateLength(
            request.username,
            "Username",
            ValidationConstants.Length.USERNAME_MIN,
            ValidationConstants.Length.USERNAME_MAX,
            errorFactory
        )?.let {
            return Result.Failure(it)
        }
        ValidationHelpers.validateUsername(request.username, errorFactory)?.let {
            return Result.Failure(it)
        }

        // Validate firstname
        ValidationHelpers.validateNotBlank(request.firstname, "First name", errorFactory)?.let {
            return Result.Failure(it)
        }
        ValidationHelpers.validateLength(
            request.firstname,
            "First name",
            ValidationConstants.Length.NAME_MIN,
            ValidationConstants.Length.NAME_MAX,
            errorFactory
        )?.let {
            return Result.Failure(it)
        }

        // Validate lastname
        ValidationHelpers.validateNotBlank(request.lastname, "Last name", errorFactory)?.let {
            return Result.Failure(it)
        }
        ValidationHelpers.validateLength(
            request.lastname,
            "Last name",
            ValidationConstants.Length.NAME_MIN,
            ValidationConstants.Length.NAME_MAX,
            errorFactory
        )?.let {
            return Result.Failure(it)
        }

        // Validate password
        ValidationHelpers.validateNotBlank(request.password, "Password", errorFactory)?.let {
            return Result.Failure(it)
        }
        ValidationHelpers.validateLength(
            request.password,
            "Password",
            minLength = ValidationConstants.Length.PASSWORD_MIN,
            errorFactory = errorFactory
        )?.let {
            return Result.Failure(it)
        }
        ValidationHelpers.validatePassword(request.password, errorFactory)?.let {
            return Result.Failure(it)
        }

        // Validate phone (optional)
        ValidationHelpers.validateOptionalField(
            request.phone,
            "Phone",
            errorFactory = errorFactory
        )?.let {
            return Result.Failure(it)
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
            return Result.Failure(it)
        }

        ValidationHelpers.validateNotBlank(
            request.password,
            "Password",
            errorFactory
        )?.let {
            return Result.Failure(it)
        }

        return Result.Success(request)
    }
}