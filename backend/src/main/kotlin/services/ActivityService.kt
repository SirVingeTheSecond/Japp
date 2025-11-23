package com.japp.services

import com.japp.models.Result
import com.japp.models.ActivityType
import com.japp.models.dto.ActivityDto
import com.japp.models.dto.GroupActivitiesDto
import com.japp.models.error.AppError
import com.japp.repositories.interfaces.IActivityRepository
import com.japp.repositories.interfaces.IGroupRepository
import com.japp.repositories.interfaces.IUserRepository
import com.japp.utils.toDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
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
                        metadata = buildMetadata(
                            "groupName" to groupName
                        )
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
                        metadata = buildMetadata(
                            "addedBy" to userId,
                            "memberName" to (newMember?.username ?: "Unknown")
                        )
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
                        metadata = buildMetadata(
                            "addedUserId" to addedUserId,
                            "addedUserName" to (addedUser?.username ?: "Unknown")
                        )
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
                        metadata = buildMetadata(
                            "memberName" to (user?.username ?: "Unknown")
                        )
                    )
                }
            } catch (_: Exception) {
                // Not critical, silently fail
            }
        }
    }

    suspend fun logMemberRemoved(groupId: Int, removedBy: Int, removedUserId: Int) {
        withContext(Dispatchers.IO) {
            try {
                transaction {
                    val removedUser = userRepository.findById(removedUserId)
                    val removerUser = userRepository.findById(removedBy)
                    activityRepository.create(
                        groupId = groupId,
                        userId = removedBy,
                        actionType = ActivityType.MEMBER_REMOVED,
                        description = "removed ${removedUser?.username ?: "Unknown"} from the group",
                        metadata = buildMetadata(
                            "removedUserId" to removedUserId,
                            "removedUserName" to (removedUser?.username ?: "Unknown"),
                            "removedBy" to removedBy,
                            "removedByName" to (removerUser?.username ?: "Unknown")
                        )
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
                        metadata = buildMetadata(
                            "amount" to amount,
                            "currency" to currency,
                            "description" to description
                        )
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
                        metadata = buildMetadata(
                            "description" to description
                        )
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
                        metadata = buildMetadata(
                            "amount" to amount,
                            "description" to description
                        )
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
                        metadata = buildMetadata(
                            "amount" to amount,
                            "toUserId" to toUserId,
                            "toUserName" to (toUser?.username ?: "Unknown")
                        )
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
                        metadata = buildMetadata(
                            "amount" to amount,
                            "fromUserId" to fromUserId,
                            "fromUserName" to (fromUser?.username ?: "Unknown")
                        )
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
                        metadata = buildMetadata(
                            "expenseDescription" to expenseDescription
                        )
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
    ): Result<GroupActivitiesDto, AppError> {
        return withContext(Dispatchers.IO) {
            try {
                transaction {
                    if (!groupRepository.isMember(groupId, userId)) {
                        return@transaction Result.Failure(AppError.NotMember(groupId))
                    }

                    val group = groupRepository.findById(groupId)
                        ?: return@transaction Result.Failure(
                            AppError.Internal("Group not found")
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
                    AppError.Internal(e.message ?: "Failed to retrieve activities")
                )
            }
        }
    }

    suspend fun getUserActivities(
        userId: Int,
        limit: Int = 50
    ): Result<List<ActivityDto>, AppError> {
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
                    AppError.Internal(e.message ?: "Failed to retrieve activities")
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

    private fun buildMetadata(vararg entries: Pair<String, Any?>): String {
        val jsonObject = buildJsonObject {
            entries.forEach { (key, value) ->
                when (value) {
                    null -> {}
                    is Number -> put(key, JsonPrimitive(value))
                    is Boolean -> put(key, JsonPrimitive(value))
                    else -> put(key, JsonPrimitive(value.toString()))
                }
            }
        }
        return jsonObject.toString()
    }
}
