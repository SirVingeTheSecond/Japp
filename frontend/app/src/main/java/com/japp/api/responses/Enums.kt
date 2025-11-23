package com.japp.api.responses

// ==============================
// EXPENSE ENUMS
// ==============================

/**
 * Defines how an expense should be split among group members.
 *
 * - [EQUAL]: Split evenly among all participants
 * - [CUSTOM]: Custom split with specified amounts or percentages
 */
enum class SplitType(val value: String) {
    EQUAL("equal"),
    CUSTOM("custom");

    companion object {
        /**
         * Parse string value to SplitType enum (case-insensitive).
         * Returns null if the value doesn't match any enum constant.
         */
        @JvmStatic
        fun fromString(value: String): SplitType? =
            entries.find { it.value.equals(value, ignoreCase = true) }
    }
}

/**
 * Expense category for classification and reporting.
 * Each category has a display name for the user interface.
 */
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
        @JvmStatic
        fun fromString(value: String): ExpenseCategory? =
            entries.find { it.value.equals(value, ignoreCase = true) }

        /**
         * Returns the default expense category.
         */
        fun default(): ExpenseCategory = OTHER
    }
}

// ==============================
// CURRENCY ENUMS
// ==============================

/**
 * Supported currencies in the application.
 * Each currency has a code (ISO 4217) and a display symbol.
 */
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

        @JvmStatic
        fun fromString(value: String) = fromCode(value)
    }
}

// ==============================
// USER ENUMS
// ==============================

/**
 * User account status.
 *
 * - [ACTIVE]: User can access the system normally
 * - [INACTIVE]: User account is disabled
 */
enum class UserStatus(val value: String) {
    ACTIVE("active"),
    INACTIVE("inactive");

    companion object {
        /**
         * Parse string value to UserStatus enum (case-insensitive).
         * Returns null if the value doesn't match any enum constant.
         */
        @JvmStatic
        fun fromString(value: String): UserStatus? =
            entries.find { it.value.equals(value, ignoreCase = true) }
    }
}

// ==============================
// GROUP ENUMS
// ==============================

/**
 * Role of a user within a group.
 *
 * - [OWNER]: Group creator with full permissions
 * - [MEMBER]: Regular group member
 */
enum class GroupRole(val value: String) {
    OWNER("owner"),
    MEMBER("member");

    companion object {
        /**
         * Parse string value to GroupRole enum (case-insensitive).
         * Returns null if the value does not match any enum constant.
         */
        @JvmStatic
        fun fromString(value: String): GroupRole? =
            entries.find { it.value.equals(value, ignoreCase = true) }
    }
}

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
enum class SettlementStatus(val value: String) {
    PENDING("pending"),
    COMPLETED("completed"),
    CANCELLED("cancelled");

    companion object {
        /**
         * Parse string value to SettlementStatus enum (case-insensitive).
         * Returns null if the value does not match any enum constant.
         */
        @JvmStatic
        fun fromString(value: String): SettlementStatus? =
            entries.find { it.value.equals(value, ignoreCase = true) }
    }
}

// ==============================
// ACTIVITY ENUMS
// ==============================

/**
 * Type of activity for audit logging and activity feeds.
 * Each type has a human-readable description.
 */
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
        @JvmStatic
        fun fromString(value: String): ActivityType? =
            entries.find { it.value.equals(value, ignoreCase = true) }
    }
}

// ==============================
// MESSAGE ENUMS
// ==============================

/**
 * Type of message in group chat.
 *
 * - [USER]: Message sent by a user
 * - [SYSTEM]: Automated system message (e.g. notifications)
 */
enum class MessageType(val value: String) {
    USER("user"),
    SYSTEM("system");

    companion object {
        /**
         * Parse string value to MessageType enum (case-insensitive).
         * Returns null if the value does not match any enum constant.
         */
        @JvmStatic
        fun fromString(value: String): MessageType? =
            entries.find { it.value.equals(value, ignoreCase = true) }
    }
}

// ==============================
// WEBSOCKET ENUMS
// ==============================

/**
 * Type of WebSocket message for chat communication.
 *
 * - [CONNECTED]: Initial connection confirmation
 * - [SUBSCRIBE]: Client requests to join a group channel
 * - [SUBSCRIBED]: Server confirms successful subscription
 * - [UNSUBSCRIBE]: Client requests to leave a group channel
 * - [UNSUBSCRIBED]: Server confirms successful unsubscription
 * - [NEW_MESSAGE]: New message broadcast to group
 * - [MESSAGE_READ]: Message read status update
 * - [MESSAGE_DELETED]: Message deletion notification
 * - [TYPING_START]: User started typing
 * - [TYPING_STOP]: User stopped typing
 * - [PING]: Heartbeat ping from server
 * - [PONG]: Heartbeat pong from client
 * - [ERROR]: Error response from server
 */
enum class WebSocketMessageType(val value: String) {
    CONNECTED("connected"),
    SUBSCRIBE("subscribe"),
    SUBSCRIBED("subscribed"),
    UNSUBSCRIBE("unsubscribe"),
    UNSUBSCRIBED("unsubscribed"),
    NEW_MESSAGE("new_message"),
    MESSAGE_SENT("message_sent"),
    MESSAGE_READ("message_read"),
    MESSAGE_DELETED("message_deleted"),
    TYPING_START("typing_start"),
    TYPING_STOP("typing_stop"),
    PING("ping"),
    PONG("pong"),
    ERROR("error");

    companion object {
        /**
         * Parse string value to WebSocketMessageType enum (case-insensitive).
         * Returns null if the value does not match any enum constant.
         */
        @JvmStatic
        fun fromString(value: String): WebSocketMessageType? =
            entries.find { it.value.equals(value, ignoreCase = true) }
    }
}