package com.japp.validation

import com.japp.models.AuthError
import com.japp.models.Result
import com.japp.models.dto.LoginRequest
import com.japp.models.dto.SignupRequest

// ToDo: This might need to be cleaned up a bit through refactoring
object AuthValidator {

    // Regex do really be lookin' disgusting
    private val EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()

    /**
     * Returns Success (request) if valid, Failure (error) otherwise
     */
    fun validateSignup(request: SignupRequest): Result<SignupRequest, AuthError> {
        // Validate email
        if (request.email.isBlank()) {
            return Result.Failure(AuthError.ValidationError("Email is required"))
        }
        if (!EMAIL_REGEX.matches(request.email)) {
            return Result.Failure(AuthError.ValidationError("Invalid email format"))
        }

        // Validate password
        if (request.password.isBlank()) {
            return Result.Failure(AuthError.ValidationError("Password is required"))
        }
        if (request.password.length < 8) {
            return Result.Failure(AuthError.ValidationError("Password must be at least 8 characters"))
        }
        if (!request.password.any { it.isDigit() }) {
            return Result.Failure(AuthError.ValidationError("Password must contain at least one digit"))
        }
        if (!request.password.any { it.isLetter() }) {
            return Result.Failure(AuthError.ValidationError("Password must contain at least one letter"))
        }

        // Validate name
        if (request.name.isBlank()) {
            return Result.Failure(AuthError.ValidationError("Name is required"))
        }
        if (request.name.length < 2) {
            return Result.Failure(AuthError.ValidationError("Name must be at least 2 characters"))
        }

        // Validate phone (optional)
        if (request.phone != null && request.phone.isBlank()) {
            return Result.Failure(AuthError.ValidationError("Phone cannot be empty if provided"))
        }

        return Result.Success(request)
    }

    /**
     * Returns Success (request) if valid, Failure (error) otherwise
     */
    fun validateLogin(request: LoginRequest): Result<LoginRequest, AuthError> {
        if (request.email.isBlank()) {
            return Result.Failure(AuthError.ValidationError("Email is required"))
        }
        if (!EMAIL_REGEX.matches(request.email)) {
            return Result.Failure(AuthError.ValidationError("Invalid email format"))
        }

        if (request.password.isBlank()) {
            return Result.Failure(AuthError.ValidationError("Password is required"))
        }

        return Result.Success(request)
    }
}