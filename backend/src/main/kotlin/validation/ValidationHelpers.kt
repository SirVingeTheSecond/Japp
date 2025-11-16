package com.japp.validation

import com.japp.models.*
import com.japp.models.error.IAppError

object ValidationHelpers {

    /**
     * Validate that a string is not blank
     */
    inline fun <reified E : IAppError> validateNotBlank(
        value: String?,
        fieldName: String,
        errorFactory: (String) -> E
    ): Result<Unit, E>? {
        if (value.isNullOrBlank()) {
            return Result.Failure(errorFactory("$fieldName is required"))
        }
        return null
    }

    /**
     * Validate string length constraints
     */
    inline fun <reified E : IAppError> validateLength(
        value: String,
        fieldName: String,
        minLength: Int? = null,
        maxLength: Int? = null,
        errorFactory: (String) -> E
    ): Result<Unit, E>? {
        minLength?.let {
            if (value.length < it) {
                return Result.Failure(errorFactory(ValidationConstants.Messages.minLength(fieldName, it)))
            }
        }

        maxLength?.let {
            if (value.length > it) {
                return Result.Failure(errorFactory(ValidationConstants.Messages.maxLength(fieldName, it)))
            }
        }

        return null
    }

    /**
     * Validate email format
     */
    inline fun <reified E : IAppError> validateEmail(
        email: String,
        errorFactory: (String) -> E
    ): Result<Unit, E>? {
        if (!ValidationConstants.Regex.EMAIL.matches(email)) {
            return Result.Failure(errorFactory(ValidationConstants.Messages.INVALID_EMAIL))
        }
        return null
    }

    /**
     * Validate username format and constraints
     */
    inline fun <reified E : IAppError> validateUsername(
        username: String,
        errorFactory: (String) -> E
    ): Result<Unit, E>? {
        // Check banned usernames
        if (username.lowercase() in ValidationConstants.Banned.USERNAMES) {
            return Result.Failure(errorFactory(ValidationConstants.Messages.BANNED_USERNAME_ROLF))
        }

        // Check format
        if (!ValidationConstants.Regex.USERNAME.matches(username)) {
            return Result.Failure(errorFactory(ValidationConstants.Messages.INVALID_USERNAME_FORMAT))
        }

        return null
    }

    /**
     * Validate password requirements
     */
    inline fun <reified E : IAppError> validatePassword(
        password: String,
        errorFactory: (String) -> E
    ): Result<Unit, E>? {
        if (!password.any { it.isDigit() }) {
            return Result.Failure(errorFactory(ValidationConstants.Messages.PASSWORD_NEEDS_DIGIT))
        }

        if (!password.any { it.isLetter() }) {
            return Result.Failure(errorFactory(ValidationConstants.Messages.PASSWORD_NEEDS_LETTER))
        }

        return null
    }

    /**
     * Validate amount is positive
     */
    inline fun <reified E : IAppError> validatePositiveAmount(
        amount: Double,
        fieldName: String = "Amount",
        errorFactory: (String) -> E
    ): Result<Unit, E>? {
        if (amount <= 0) {
            return Result.Failure(errorFactory(ValidationConstants.Messages.amountMustBePositive(fieldName)))
        }
        return null
    }

    /**
     * Validate optional field (only if provided)
     */
    inline fun <reified E : IAppError> validateOptionalField(
        value: String?,
        fieldName: String,
        minLength: Int? = null,
        maxLength: Int? = null,
        errorFactory: (String) -> E
    ): Result<Unit, E>? {
        value?.let {
            if (it.isBlank()) {
                return Result.Failure(errorFactory(ValidationConstants.Messages.cannotBeBlankIfProvided(fieldName)))
            }

            return validateLength(it, fieldName, minLength, maxLength, errorFactory)
        }
        return null
    }

    /**
     * Validate enum value
     */
    inline fun <reified E : IAppError, reified T : Enum<T>> validateEnum(
        value: String?,
        enumName: String,
        errorFactory: (String) -> E,
        fromString: (String) -> T?
    ): Result<T, E>? {
        if (value == null) return null

        val enumValue = fromString(value)
            ?: return Result.Failure(errorFactory("Invalid $enumName: $value"))

        return Result.Success(enumValue)
    }
}