package com.japp.services

import com.japp.models.Result
import com.japp.models.ActivityType
import com.japp.models.dto.ActivityDto
import com.japp.models.dto.GroupActivitiesDto
import com.japp.models.error.ActivityError
import com.japp.repositories.interfaces.IActivityRepository
import com.japp.repositories.interfaces.IGroupRepository
import com.japp.repositories.interfaces.IUserRepository
import com.japp.utils.toDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class ActivityService(
    private val activityRepository: IActivityRepository,
    private val userRepository: IUserRepository,
    private val groupRepository: IGroupRepository
) {

    suspend fun logGroupCreated(groupId: Int, userId: Int, groupName: String) {
        withContext(Dispatchers.IO) {
            try {
                transaction {
                    activityRepository.create(
                        groupId = groupId,
                        userId = userId,
                        actionType = ActivityType.GROUP_CREATED,
                        description = "created the group",
                        metadata = """{"groupName":"$groupName"}"""
                    )
                }
            } catch (_: Exception) {
                // Not critical, silently fail
            }
        }
    }

    suspend fun logMemberJoined(groupId: Int, userId: Int, newMemberId: Int) {
        withContext(Dispatchers.IO) {
            try {
                transaction {
                    val newMember = userRepository.findById(newMemberId)
                    activityRepository.create(
                        groupId = groupId,
                        userId = newMemberId,
                        actionType = ActivityType.MEMBER_JOINED,
                        description = "joined the group",
                        metadata = """{"addedBy":$userId,"memberName":"${newMember?.username ?: "Unknown"}"}"""
                    )
                }
            } catch (_: Exception) {
                // Not critical, silently fail
            }
        }
    }

    suspend fun logMemberAdded(groupId: Int, addedBy: Int, addedUserId: Int) {
        withContext(Dispatchers.IO) {
            try {
                transaction {
                    val addedUser = userRepository.findById(addedUserId)
                    activityRepository.create(
                        groupId = groupId,
                        userId = addedBy,
                        actionType = ActivityType.MEMBER_JOINED,
                        description = "added ${addedUser?.username ?: "Unknown"} to the group",
                        metadata = """{"addedUserId":$addedUserId,"addedUserName":"${addedUser?.username ?: "Unknown"}"}"""
                    )
                }
            } catch (_: Exception) {
                // Not critical, silently fail
            }
        }
    }

    suspend fun logMemberLeft(groupId: Int, userId: Int) {
        withContext(Dispatchers.IO) {
            try {
                transaction {
                    val user = userRepository.findById(userId)
                    activityRepository.create(
                        groupId = groupId,
                        userId = userId,
                        actionType = ActivityType.MEMBER_LEFT,
                        description = "left the group",
                        metadata = """{"memberName":"${user?.username ?: "Unknown"}"}"""
                    )
                }
            } catch (_: Exception) {
                // Not critical, silently fail
            }
        }
    }

    suspend fun logExpenseCreated(
        groupId: Int,
        userId: Int,
        expenseId: Int,
        amount: Double,
        currency: String,
        description: String
    ) {
        withContext(Dispatchers.IO) {
            try {
                transaction {
                    activityRepository.create(
                        groupId = groupId,
                        userId = userId,
                        actionType = ActivityType.EXPENSE_CREATED,
                        description = "added expense: $description",
                        relatedExpenseId = expenseId,
                        metadata = """{"amount":$amount,"currency":"$currency","description":"$description"}"""
                    )
                }
            } catch (_: Exception) {
                // Not critical, silently fail
            }
        }
    }

    suspend fun logExpenseUpdated(
        groupId: Int,
        userId: Int,
        expenseId: Int,
        description: String
    ) {
        withContext(Dispatchers.IO) {
            try {
                transaction {
                    activityRepository.create(
                        groupId = groupId,
                        userId = userId,
                        actionType = ActivityType.EXPENSE_UPDATED,
                        description = "updated expense: $description",
                        relatedExpenseId = expenseId,
                        metadata = """{"description":"$description"}"""
                    )
                }
            } catch (_: Exception) {
                // Not critical, silently fail
            }
        }
    }

    suspend fun logExpenseDeleted(
        groupId: Int,
        userId: Int,
        expenseId: Int,
        amount: Double,
        description: String
    ) {
        withContext(Dispatchers.IO) {
            try {
                transaction {
                    activityRepository.create(
                        groupId = groupId,
                        userId = userId,
                        actionType = ActivityType.EXPENSE_DELETED,
                        description = "deleted expense: $description",
                        relatedExpenseId = expenseId,
                        metadata = """{"amount":$amount,"description":"$description"}"""
                    )
                }
            } catch (_: Exception) {
                // Not critical, silently fail
            }
        }
    }

    suspend fun logSettlementCreated(
        groupId: Int,
        userId: Int,
        settlementId: Int,
        toUserId: Int,
        amount: Double
    ) {
        withContext(Dispatchers.IO) {
            try {
                transaction {
                    val toUser = userRepository.findById(toUserId)
                    activityRepository.create(
                        groupId = groupId,
                        userId = userId,
                        actionType = ActivityType.SETTLEMENT_CREATED,
                        description = "created payment to ${toUser?.username ?: "Unknown"}",
                        relatedSettlementId = settlementId,
                        metadata = """{"amount":$amount,"toUserId":$toUserId,"toUserName":"${toUser?.username ?: "Unknown"}"}"""
                    )
                }
            } catch (_: Exception) {
                // Not critical, silently fail
            }
        }
    }

    suspend fun logSettlementCompleted(
        groupId: Int,
        userId: Int,
        settlementId: Int,
        fromUserId: Int,
        amount: Double
    ) {
        withContext(Dispatchers.IO) {
            try {
                transaction {
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
            } catch (_: Exception) {
                // Not critical, silently fail
            }
        }
    }

    suspend fun logReceiptUploaded(
        groupId: Int,
        userId: Int,
        expenseId: Int,
        expenseDescription: String
    ) {
        withContext(Dispatchers.IO) {
            try {
                transaction {
                    activityRepository.create(
                        groupId = groupId,
                        userId = userId,
                        actionType = ActivityType.RECEIPT_UPLOADED,
                        description = "added receipt for: $expenseDescription",
                        relatedExpenseId = expenseId,
                        metadata = """{"expenseDescription":"$expenseDescription"}"""
                    )
                }
            } catch (_: Exception) {
                // Not critical, silently fail
            }
        }
    }

    suspend fun getGroupActivities(
        groupId: Int,
        userId: Int,
        limit: Int = 50
    ): Result<GroupActivitiesDto, ActivityError> {
        return withContext(Dispatchers.IO) {
            try {
                transaction {
                    if (!groupRepository.isMember(groupId, userId)) {
                        return@transaction Result.Failure(ActivityError.NotMember(groupId))
                    }

                    val group = groupRepository.findById(groupId)
                        ?: return@transaction Result.Failure(
                            ActivityError.InternalError("Group not found")
                        )

                    val activities = activityRepository.findByGroupId(groupId, limit)

                    val activityDtos = activities.map { activity ->
                        val user = userRepository.findById(activity.userId)
                        val metadataMap = parseMetadata(activity.metadata)

                        activity.toDto(
                            userName = user?.username ?: "Unknown",
                            metadata = metadataMap
                        )
                    }

                    Result.Success(
                        GroupActivitiesDto(
                            groupId = groupId,
                            groupName = group.name,
                            activities = activityDtos
                        )
                    )
                }
            } catch (e: Exception) {
                Result.Failure(
                    ActivityError.InternalError(e.message ?: "Failed to retrieve activities")
                )
            }
        }
    }

    suspend fun getUserActivities(
        userId: Int,
        limit: Int = 50
    ): Result<List<ActivityDto>, ActivityError> {
        return withContext(Dispatchers.IO) {
            try {
                transaction {
                    val activities = activityRepository.findByUserId(userId, limit)

                    val activityDtos = activities.map { activity ->
                        val user = userRepository.findById(activity.userId)
                        val metadataMap = parseMetadata(activity.metadata)

                        activity.toDto(
                            userName = user?.username ?: "Unknown",
                            metadata = metadataMap
                        )
                    }

                    Result.Success(activityDtos)
                }
            } catch (e: Exception) {
                Result.Failure(
                    ActivityError.InternalError(e.message ?: "Failed to retrieve activities")
                )
            }
        }
    }

    private fun parseMetadata(jsonString: String): Map<String, String> {
        return try {
            val json = Json.parseToJsonElement(jsonString).jsonObject
            json.mapValues { it.value.jsonPrimitive.content }
        } catch (_: Exception) {
            emptyMap()
        }
    }
}