package com.japp.services

import com.japp.models.ActivityType
import com.japp.models.dto.ActivityDto
import com.japp.models.dto.GroupActivitiesDto
import com.japp.repositories.IActivityRepository
import com.japp.repositories.IGroupRepository
import com.japp.repositories.IUserRepository
import com.japp.utils.toDto
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class ActivityService(
    private val activityRepository: IActivityRepository,
    private val groupRepository: IGroupRepository,
    private val userRepository: IUserRepository
) {

    fun logGroupCreated(groupId: Int, userId: Int, groupName: String) {
        activityRepository.create(
            groupId = groupId,
            userId = userId,
            actionType = ActivityType.GROUP_CREATED,
            description = "created the group",
            metadata = """{"groupName":"$groupName"}"""
        )
    }

    fun logMemberJoined(groupId: Int, userId: Int, joinedUserId: Int) {
        val joinedUser = userRepository.findById(joinedUserId)
        activityRepository.create(
            groupId = groupId,
            userId = userId,
            actionType = ActivityType.MEMBER_JOINED,
            description = "joined the group",
            metadata = """{"joinedUserId":$joinedUserId,"joinedUserName":"${joinedUser?.username ?: "Unknown"}"}"""
        )
    }

    fun logMemberLeft(groupId: Int, userId: Int) {
        activityRepository.create(
            groupId = groupId,
            userId = userId,
            actionType = ActivityType.MEMBER_LEFT,
            description = "left the group",
            metadata = "{}"
        )
    }

    fun logExpenseCreated(
        groupId: Int,
        userId: Int,
        expenseId: Int,
        amount: Double,
        currency: String,
        description: String
    ) {
        activityRepository.create(
            groupId = groupId,
            userId = userId,
            actionType = ActivityType.EXPENSE_CREATED,
            description = "added expense: $description",
            relatedExpenseId = expenseId,
            metadata = """{"amount":$amount,"currency":"$currency","description":"$description"}"""
        )
    }

    fun logExpenseDeleted(
        groupId: Int,
        userId: Int,
        expenseId: Int,
        amount: Double,
        description: String
    ) {
        activityRepository.create(
            groupId = groupId,
            userId = userId,
            actionType = ActivityType.EXPENSE_DELETED,
            description = "deleted expense: $description",
            relatedExpenseId = expenseId,
            metadata = """{"amount":$amount,"description":"$description"}"""
        )
    }

    fun logSettlementCreated(
        groupId: Int,
        userId: Int,
        settlementId: Int,
        toUserId: Int,
        amount: Double
    ) {
        val toUser = userRepository.findById(toUserId)
        activityRepository.create(
            groupId = groupId,
            userId = userId,
            actionType = ActivityType.SETTLEMENT_CREATED,
            description = "recorded payment to ${toUser?.username ?: "Unknown"}",
            relatedSettlementId = settlementId,
            metadata = """{"amount":$amount,"toUserId":$toUserId,"toUserName":"${toUser?.username ?: "Unknown"}"}"""
        )
    }

    fun logSettlementCompleted(
        groupId: Int,
        userId: Int,
        settlementId: Int,
        fromUserId: Int,
        amount: Double
    ) {
        val fromUser = userRepository.findById(fromUserId)
        activityRepository.create(
            groupId = groupId,
            userId = userId,
            actionType = ActivityType.SETTLEMENT_COMPLETED,
            description = "confirmed payment from ${fromUser?.username ?: "Unknown"}",
            relatedSettlementId = settlementId,
            metadata = """{"amount":$amount,"fromUserId":$fromUserId,"fromUserName":"${fromUser?.username ?: "Unknown"}"}"""
        )
    }

    fun logReceiptUploaded(
        groupId: Int,
        userId: Int,
        expenseId: Int,
        expenseDescription: String
    ) {
        activityRepository.create(
            groupId = groupId,
            userId = userId,
            actionType = ActivityType.RECEIPT_UPLOADED,
            description = "added receipt for: $expenseDescription",
            relatedExpenseId = expenseId,
            metadata = """{"expenseDescription":"$expenseDescription"}"""
        )
    }

    fun getGroupActivities(groupId: Int, limit: Int = 50): GroupActivitiesDto {
        val group = groupRepository.findById(groupId)
        val activities = activityRepository.findByGroupId(groupId, limit)

        val activityDtos = activities.map { activity ->
            val user = userRepository.findById(activity.userId)
            val metadataMap = parseMetadata(activity.metadata)

            activity.toDto(
                userName = user?.username ?: "Unknown",
                metadata = metadataMap
            )
        }

        return GroupActivitiesDto(
            groupId = groupId,
            groupName = group?.name ?: "Unknown",
            activities = activityDtos
        )
    }

    private fun parseMetadata(jsonString: String): Map<String, String> {
        return try {
            val json = Json.parseToJsonElement(jsonString).jsonObject
            json.mapValues { it.value.jsonPrimitive.content }
        } catch (e: Exception) {
            emptyMap()
        }
    }
}