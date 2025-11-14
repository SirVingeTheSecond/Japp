package com.japp.models

import kotlinx.serialization.Serializable

/**
 * Split type for expense distribution
 */
@Serializable
enum class SplitType(val value: String) {
    EQUAL("equal"),
    CUSTOM("custom");

    companion object {
        fun fromString(value: String): SplitType? {
            return entries.find { it.value.equals(value, ignoreCase = true) }
        }
    }
}

@Serializable
enum class Currency(val code: String, val symbol: String) {
    DKK("DKK", "kr"),
    EUR("EUR", "€");

    companion object {
        fun fromCode(code: String): Currency? {
            return entries.find { it.code.equals(code, ignoreCase = true) }
        }
    }
}

@Serializable
enum class UserStatus(val value: String) {
    ACTIVE("active"),
    INACTIVE("inactive");

    companion object {
        fun fromString(value: String): UserStatus? {
            return entries.find { it.value.equals(value, ignoreCase = true) }
        }
    }
}

@Serializable
enum class GroupRole(val value: String) {
    OWNER("owner"),
    MEMBER("member");

    companion object {
        fun fromString(value: String): GroupRole? {
            return entries.find { it.value.equals(value, ignoreCase = true) }
        }
    }
}

@Serializable
enum class ActivityType(val value: String, val description: String) {
    GROUP_CREATED("group_created", "Group created"),
    MEMBER_JOINED("member_joined", "Member joined"),
    MEMBER_LEFT("member_left", "Member left"),
    EXPENSE_CREATED("expense_created", "Expense created"),
    EXPENSE_UPDATED("expense_updated", "Expense updated"),
    EXPENSE_DELETED("expense_deleted", "Expense deleted"),
    SETTLEMENT_CREATED("settlement_created", "Settlement created"),
    SETTLEMENT_COMPLETED("settlement_completed", "Settlement completed"),
    RECEIPT_UPLOADED("receipt_uploaded", "Receipt uploaded");

    companion object {
        fun fromString(value: String): ActivityType? {
            return entries.find { it.value.equals(value, ignoreCase = true) }
        }
    }
}

@Serializable
enum class SettlementStatus(val value: String) {
    PENDING("pending"),
    COMPLETED("completed"),
    CANCELLED("cancelled");

    companion object {
        fun fromString(value: String): SettlementStatus? {
            return entries.find { it.value.equals(value, ignoreCase = true) }
        }
    }
}

@Serializable
enum class ExpenseCategory(val value: String, val displayName: String) {
    FOOD("food", "Food and Dining"),
    TRANSPORTATION("transportation", "Transportation"),
    ACCOMMODATION("accommodation", "Accommodation"),
    ENTERTAINMENT("entertainment", "Entertainment"),
    SHOPPING("shopping", "Shopping"),
    GROCERIES("groceries", "Groceries"),
    UTILITIES("utilities", "Utilities"),
    OTHER("other", "Other");

    companion object {
        fun fromString(value: String): ExpenseCategory? {
            return entries.find { it.value.equals(value, ignoreCase = true) }
        }

        fun default(): ExpenseCategory = OTHER
    }
}