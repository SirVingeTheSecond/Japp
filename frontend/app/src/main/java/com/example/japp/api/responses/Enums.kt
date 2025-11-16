package com.example.japp.api.responses

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Split type for expense distribution
 */
@Serializable(with = SplitTypeSerializer::class)
enum class SplitType(val value: String) {
    EQUAL("equal"),
    CUSTOM("custom");

    companion object {
        fun fromString(value: String): SplitType? {
            return entries.find { it.value.equals(value, ignoreCase = true) }
        }
    }
}

object SplitTypeSerializer : KSerializer<SplitType> {
    override val descriptor = PrimitiveSerialDescriptor("SplitType", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: SplitType) {
        encoder.encodeString(value.value)
    }

    override fun deserialize(decoder: Decoder): SplitType {
        return SplitType.fromString(decoder.decodeString())
            ?: throw SerializationException("Unknown split type")
    }
}

@Serializable(with = CurrencySerializer::class)
enum class Currency(val code: String, val symbol: String) {
    DKK("DKK", "kr"),
    EUR("EUR", "€");

    companion object {
        fun fromCode(code: String): Currency? {
            return entries.find { it.code.equals(code, ignoreCase = true) }
        }
    }
}

object CurrencySerializer : KSerializer<Currency> {
    override val descriptor = PrimitiveSerialDescriptor("Currency", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Currency) {
        encoder.encodeString(value.code)
    }

    override fun deserialize(decoder: Decoder): Currency {
        return Currency.fromCode(decoder.decodeString())
            ?: throw SerializationException("Unknown currency")
    }
}

@Serializable(with = UserStatusSerializer::class)
enum class UserStatus(val value: String) {
    ACTIVE("active"),
    INACTIVE("inactive");

    companion object {
        fun fromString(value: String): UserStatus? {
            return entries.find { it.value.equals(value, ignoreCase = true) }
        }
    }
}

object UserStatusSerializer : KSerializer<UserStatus> {
    override val descriptor = PrimitiveSerialDescriptor("UserStatus", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: UserStatus) {
        encoder.encodeString(value.value)
    }

    override fun deserialize(decoder: Decoder): UserStatus {
        return UserStatus.fromString(decoder.decodeString())
            ?: throw SerializationException("Unknown user status")
    }
}

@Serializable(with = GroupRoleSerializer::class)
enum class GroupRole(val value: String) {
    OWNER("owner"),
    MEMBER("member");

    companion object {
        fun fromString(value: String): GroupRole? {
            return entries.find { it.value.equals(value, ignoreCase = true) }
        }
    }
}

object GroupRoleSerializer : KSerializer<GroupRole> {
    override val descriptor = PrimitiveSerialDescriptor("GroupRole", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: GroupRole) {
        encoder.encodeString(value.value)
    }

    override fun deserialize(decoder: Decoder): GroupRole {
        return GroupRole.fromString(decoder.decodeString())
            ?: throw SerializationException("Unknown group role")
    }
}

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
        fun fromString(value: String): ActivityType? {
            return entries.find { it.value.equals(value, ignoreCase = true) }
        }
    }
}

object ActivityTypeSerializer : KSerializer<ActivityType> {
    override val descriptor = PrimitiveSerialDescriptor("ActivityType", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ActivityType) {
        encoder.encodeString(value.value)
    }

    override fun deserialize(decoder: Decoder): ActivityType {
        return ActivityType.fromString(decoder.decodeString())
            ?: throw SerializationException("Unknown activity type")
    }
}

@Serializable(with = SettlementStatusSerializer::class)
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

object SettlementStatusSerializer : KSerializer<SettlementStatus> {
    override val descriptor = PrimitiveSerialDescriptor("SettlementStatus", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: SettlementStatus) {
        encoder.encodeString(value.value)
    }

    override fun deserialize(decoder: Decoder): SettlementStatus {
        return SettlementStatus.fromString(decoder.decodeString())
            ?: throw SerializationException("Unknown settlement status")
    }
}

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
        fun fromString(value: String): ExpenseCategory? {
            return entries.find { it.value.equals(value, ignoreCase = true) }
        }

        fun default(): ExpenseCategory = OTHER
    }
}

object ExpenseCategorySerializer : KSerializer<ExpenseCategory> {
    override val descriptor = PrimitiveSerialDescriptor("ExpenseCategory", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ExpenseCategory) {
        encoder.encodeString(value.value)
    }

    override fun deserialize(decoder: Decoder): ExpenseCategory {
        return ExpenseCategory.fromString(decoder.decodeString())
            ?: throw SerializationException("Unknown expense category")
    }
}

@Serializable(with = MessageTypeSerializer::class)
enum class MessageType(val value: String) {
    USER("user"),
    SYSTEM("system");

    companion object {
        fun fromString(value: String): MessageType? {
            return entries.find { it.value.equals(value, ignoreCase = true) }
        }
    }
}

object MessageTypeSerializer : KSerializer<MessageType> {
    override val descriptor = PrimitiveSerialDescriptor("MessageType", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: MessageType) {
        encoder.encodeString(value.value)
    }

    override fun deserialize(decoder: Decoder): MessageType {
        return MessageType.fromString(decoder.decodeString())
            ?: throw SerializationException("Unknown message type")
    }
}