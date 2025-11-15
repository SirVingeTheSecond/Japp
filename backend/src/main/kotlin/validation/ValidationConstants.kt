package com.japp.validation

import com.japp.models.*

object ValidationConstants {

    // Regex patterns
    object Regex {
        val EMAIL = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
        val USERNAME = "^[A-Za-z0-9_]+$".toRegex()
        val BANNED_USERNAME = "^(?!(?i)rolf$)[A-Za-z0-9_]+$".toRegex()
    }

    // String length constraints
    object Length {
        const val USERNAME_MIN = 3
        const val USERNAME_MAX = 20

        const val NAME_MIN = 2
        const val NAME_MAX = 100

        const val PASSWORD_MIN = 8

        const val GROUP_NAME_MIN = 2
        const val GROUP_NAME_MAX = 100

        const val EXPENSE_DESCRIPTION_MAX = 500

        const val INVITE_CODE_LENGTH = 6
    }

    // Numeric constraints
    object Amount {
        const val MIN = 0.01
        const val PERCENTAGE_TOTAL = 100.0
        const val COMPARISON_TOLERANCE = 0.01
    }

    // Enum values
    object Enums {
        val VALID_SPLIT_TYPES = SplitType.entries.map { it.value }
        val VALID_CURRENCIES = Currency.entries.map { it.code }
        val VALID_EXPENSE_CATEGORIES = ExpenseCategory.entries.map { it.value }
        val VALID_USER_STATUSES = UserStatus.entries.map { it.value }
        val VALID_GROUP_ROLES = GroupRole.entries.map { it.value }
    }

    object Banned {
        val USERNAMES = setOf("rolf") // Case-insensitive
    }

    // Error messages
    object Messages {
        // Required fields
        const val REQUIRED_EMAIL = "Email is required"
        const val REQUIRED_USERNAME = "Username is required"
        const val REQUIRED_FIRSTNAME = "First name is required"
        const val REQUIRED_LASTNAME = "Last name is required"
        const val REQUIRED_PASSWORD = "Password is required"
        const val REQUIRED_GROUP_NAME = "Group name is required"
        const val REQUIRED_DESCRIPTION = "Description is required"
        const val REQUIRED_INVITE_CODE = "Invite code is required"

        // Format errors
        const val INVALID_EMAIL = "Invalid email format"
        const val INVALID_USERNAME_FORMAT = "Username can only contain letters, numbers, and underscores"
        const val INVALID_INVITE_CODE = "Invalid invite code"
        const val INVALID_SPLIT_TYPE = "Invalid split type"
        const val INVALID_CURRENCY = "Invalid currency code"
        const val INVALID_EXPENSE_CATEGORY = "Invalid expense category"

        // Length errors
        fun minLength(field: String, min: Int) = "$field must be at least $min characters"
        fun maxLength(field: String, max: Int) = "$field must not exceed $max characters"
        fun exactLength(field: String, length: Int) = "$field must be exactly $length characters"

        // Blank errors
        fun cannotBeBlank(field: String) = "$field cannot be blank"
        fun cannotBeBlankIfProvided(field: String) = "$field cannot be blank if provided"

        // Amount errors
        fun amountMustBePositive(field: String = "Amount") = "$field must be greater than 0"
        const val SPLIT_AMOUNTS_MISMATCH = "Split amounts must sum to total expense amount"
        const val SPLIT_PERCENTAGES_MISMATCH = "Split percentages must sum to 100%"

        // Password requirements
        const val PASSWORD_NEEDS_DIGIT = "Password must contain at least one digit"
        const val PASSWORD_NEEDS_LETTER = "Password must contain at least one letter"

        // Business logic
        const val USERNAME_TAKEN = "Username already taken"
        const val BANNED_USERNAME_ROLF = "Yeah Rolf, you are not allowed in here"
        const val AT_LEAST_ONE_FIELD = "At least one field must be provided for update"
        const val CUSTOM_SPLIT_NEEDS_DATA = "Custom split requires splits data"
        const val CANNOT_PAY_YOURSELF = "Cannot create settlement to yourself"
    }
}