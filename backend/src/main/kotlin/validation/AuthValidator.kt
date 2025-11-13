package com.japp.validation

import com.japp.models.AuthError
import com.japp.models.Result
import com.japp.models.dto.LoginRequest
import com.japp.models.dto.SignupRequest

// ToDo: This might need to be cleaned up a bit through refactoring
// ToDo: Make most of these values constant and use them in the messages
object AuthValidator {

    // Regex do really be lookin' disgusting
    private val EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
    private val USERNAME_REGEX = "^[A-Za-z0-9_]+$".toRegex()
    private val ROLF_REGEX = "^(?!(?i)rolf$)[A-Za-z0-9_]+$".toRegex() // ToDo: Make special feedback for this case

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

        // Validate username
        val u = request.username
        if (u.isBlank()) {
            return Result.Failure(AuthError.ValidationError("Username is required"))
        }
        if (u.length < 3) {
            return Result.Failure(AuthError.ValidationError("Username must be at least 3 characters"))
        }
        if (u.length > 20) {
            return Result.Failure(AuthError.ValidationError("Username must not exceed 20 characters"))
        }
        if (u.equals("rolf", ignoreCase = true)) {
            return Result.Failure(AuthError.ValidationError("Yeah Rolf, you are not allowed in here"))
        }
        if (!USERNAME_REGEX.matches(u)) {
            return Result.Failure(AuthError.ValidationError("Username can only contain letters, numbers, and underscores"))
        }

        // Firstname and lastname
        if (request.firstname.isBlank()) {
            return Result.Failure(AuthError.ValidationError("First name is required"))
        }
        if (request.firstname.length < 2) {
            return Result.Failure(AuthError.ValidationError("First name must be at least 2 characters"))
        }
        if (request.lastname.isBlank()) {
            return Result.Failure(AuthError.ValidationError("Last name is required"))
        }
        if (request.lastname.length < 2) {
            return Result.Failure(AuthError.ValidationError("Last name must be at least 2 characters"))
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

        // Validate phone (optional) | Ambiguous?
        if (request.phone != null && request.phone.isBlank()) {
            return Result.Failure(AuthError.ValidationError("Phone cannot be empty if provided"))
        }

        return Result.Success(request)
    }

    /**
     * Returns Success (request) if valid, Failure (error) otherwise
     */
    fun validateLogin(request: LoginRequest): Result<LoginRequest, AuthError> {
        if (request.emailOrUsername.isBlank()) {
            return Result.Failure(AuthError.ValidationError("Email or username is required"))
        }

        if (request.password.isBlank()) {
            return Result.Failure(AuthError.ValidationError("Password is required"))
        }

        return Result.Success(request)
    }
}