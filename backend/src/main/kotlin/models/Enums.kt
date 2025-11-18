package com.japp.models

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Base serializer for string enums.
 * Provides common serialization logic to reduce boilerplate across enum types.
 *
 * @param T The enum type to serialize
 * @param enumName The name of the enum for error messages
 * @param fromString Function to parse string value to enum
 * @param toString Function to convert enum to string value
 */
abstract class StringEnumSerializer<T : Enum<T>>(
    private val enumName: String,
    private val fromString: (String) -> T?,
    private val toString: (T) -> String
) : KSerializer<T> {
    override val descriptor = PrimitiveSerialDescriptor(enumName, PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: T) {
        encoder.encodeString(toString(value))
    }

    override fun deserialize(decoder: Decoder): T {
        val stringValue = decoder.decodeString()
        return fromString(stringValue)
            ?: throw SerializationException("Unknown $enumName value: '$stringValue'")
    }
}

// ==============================
// EXPENSE ENUMS
// ==============================

/**
 * Defines how an expense should be split among group members.
 *
 * - [EQUAL]: Split evenly among all participants
 * - [CUSTOM]: Custom split with specified amounts or percentages
 */
@Serializable(with = SplitTypeSerializer::class)
enum class SplitType(val value: String) {
    EQUAL("equal"),
    CUSTOM("custom");

    companion object {
        /**
         * Parse string value to SplitType enum (case-insensitive).
         * Returns null if the value doesn't match any enum constant.
         */
        fun fromString(value: String): SplitType? =
            entries.find { it.value.equals(value, ignoreCase = true) }
    }
}

object SplitTypeSerializer : StringEnumSerializer<SplitType>(
    enumName = "SplitType",
    fromString = SplitType::fromString,
    toString = { it.value }
)

/**
 * Expense category for classification and reporting.
 * Each category has a display name for the user interface.
 */
@Serializable(with = ExpenseCategorySerializer::class)
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
        /**
         * Parse string value to ExpenseCategory enum (case-insensitive).
         * Returns null if the value doesn't match any enum constant.
         */
        fun fromString(value: String): ExpenseCategory? =
            entries.find { it.value.equals(value, ignoreCase = true) }

        /**
         * Returns the default expense category.
         */
        fun default(): ExpenseCategory = OTHER
    }
}

object ExpenseCategorySerializer : StringEnumSerializer<ExpenseCategory>(
    enumName = "ExpenseCategory",
    fromString = ExpenseCategory::fromString,
    toString = { it.value }
)

// ==============================
// CURRENCY ENUMS
// ==============================

/**
 * Supported currencies in the application.
 * Each currency has a code (ISO 4217) and a display symbol.
 */
@Serializable(with = CurrencySerializer::class)
enum class Currency(val code: String, val symbol: String) {
    DKK("DKK", "kr"),
    EUR("EUR", "€");

    companion object {
        /**
         * Parse currency code to Currency enum (case-insensitive).
         * Returns null if the code doesn't match any enum constant.
         */
        fun fromCode(code: String): Currency? =
            entries.find { it.code.equals(code, ignoreCase = true) }
    }
}

object CurrencySerializer : StringEnumSerializer<Currency>(
    enumName = "Currency",
    fromString = Currency::fromCode,
    toString = { it.code }
)

// ==============================
// USER ENUMS
// ==============================

/**
 * User account status.
 *
 * - [ACTIVE]: User can access the system normally
 * - [INACTIVE]: User account is disabled
 */
@Serializable(with = UserStatusSerializer::class)
enum class UserStatus(val value: String) {
    ACTIVE("active"),
    INACTIVE("inactive");

    companion object {
        /**
         * Parse string value to UserStatus enum (case-insensitive).
         * Returns null if the value doesn't match any enum constant.
         */
        fun fromString(value: String): UserStatus? =
            entries.find { it.value.equals(value, ignoreCase = true) }
    }
}

object UserStatusSerializer : StringEnumSerializer<UserStatus>(
    enumName = "UserStatus",
    fromString = UserStatus::fromString,
    toString = { it.value }
)

// ==============================
// GROUP ENUMS
// ==============================

/**
 * Role of a user within a group.
 *
 * - [OWNER]: Group creator with full permissions
 * - [MEMBER]: Regular group member
 */
@Serializable(with = GroupRoleSerializer::class)
enum class GroupRole(val value: String) {
    OWNER("owner"),
    MEMBER("member");

    companion object {
        /**
         * Parse string value to GroupRole enum (case-insensitive).
         * Returns null if the value does not match any enum constant.
         */
        fun fromString(value: String): GroupRole? =
            entries.find { it.value.equals(value, ignoreCase = true) }
    }
}

object GroupRoleSerializer : StringEnumSerializer<GroupRole>(
    enumName = "GroupRole",
    fromString = GroupRole::fromString,
    toString = { it.value }
)

// ==============================
// SETTLEMENT ENUMS
// ==============================

/**
 * Settlement transaction status.
 *
 * - [PENDING]: Settlement created but not yet completed
 * - [COMPLETED]: Settlement has been paid
 * - [CANCELLED]: Settlement was cancelled
 */
@Serializable(with = SettlementStatusSerializer::class)
enum class SettlementStatus(val value: String) {
    PENDING("pending"),
    COMPLETED("completed"),
    CANCELLED("cancelled");

    companion object {
        /**
         * Parse string value to SettlementStatus enum (case-insensitive).
         * Returns null if the value does not match any enum constant.
         */
        fun fromString(value: String): SettlementStatus? =
            entries.find { it.value.equals(value, ignoreCase = true) }
    }
}

object SettlementStatusSerializer : StringEnumSerializer<SettlementStatus>(
    enumName = "SettlementStatus",
    fromString = SettlementStatus::fromString,
    toString = { it.value }
)

// ==============================
// ACTIVITY ENUMS
// ==============================

/**
 * Type of activity for audit logging and activity feeds.
 * Each type has a human-readable description.
 */
@Serializable(with = ActivityTypeSerializer::class)
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
        /**
         * Parse string value to ActivityType enum (case-insensitive).
         * Returns null if the value does not match any enum constant.
         */
        fun fromString(value: String): ActivityType? =
            entries.find { it.value.equals(value, ignoreCase = true) }
    }
}

object ActivityTypeSerializer : StringEnumSerializer<ActivityType>(
    enumName = "ActivityType",
    fromString = ActivityType::fromString,
    toString = { it.value }
)

// ==============================
// MESSAGE ENUMS
// ==============================

/**
 * Type of message in group chat.
 *
 * - [USER]: Message sent by a user
 * - [SYSTEM]: Automated system message (e.g. notifications)
 */
@Serializable(with = MessageTypeSerializer::class)
enum class MessageType(val value: String) {
    USER("user"),
    SYSTEM("system");

    companion object {
        /**
         * Parse string value to MessageType enum (case-insensitive).
         * Returns null if the value does not match any enum constant.
         */
        fun fromString(value: String): MessageType? =
            entries.find { it.value.equals(value, ignoreCase = true) }
    }
}

object MessageTypeSerializer : StringEnumSerializer<MessageType>(
    enumName = "MessageType",
    fromString = MessageType::fromString,
    toString = { it.value }
)