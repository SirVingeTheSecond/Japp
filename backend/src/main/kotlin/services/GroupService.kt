package com.japp.services

import com.japp.models.*
import com.japp.models.dto.*
import com.japp.models.error.AppError
import com.japp.services.interfaces.IDebtHistoryRepository
import com.japp.services.interfaces.IExpenseRepository
import com.japp.services.interfaces.IGroupRepository
import com.japp.services.interfaces.IUserRepository
import com.japp.utils.createDebtHistoryDto
import com.japp.validation.GroupValidator
import com.japp.utils.toDto
import com.japp.utils.createGroupMemberDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class GroupService(
    private val groupRepository: IGroupRepository,
    private val userRepository: IUserRepository,
    private val activityService: ActivityService,
    private val messageService: MessageService,
    private val expenseRepository: IExpenseRepository,
    private val debtHistoryRepository: IDebtHistoryRepository,
    private val notificationService: NotificationService
) {

    /**
     * Create a new group
     */
    suspend fun createGroup(
        request: CreateGroupRequest,
        userId: Int
    ): Result<GroupDto, AppError> {
        return when (val validation = GroupValidator.validateCreateGroup(request)) {
            is Result.Failure -> validation
            is Result.Success -> {
                withContext(Dispatchers.IO) {
                    try {
                        val result = transaction {
                            userRepository.findById(userId)
                                ?: return@transaction Result.Failure(
                                    AppError.Internal("User not found")
                                )

                            val group = groupRepository.create(
                                name = request.name,
                                description = request.description,
                                createdBy = userId
                            )

                            Result.Success(group.toDto())
                        }

                        if (result is Result.Success) {
                            activityService.logGroupCreated(result.value.id, userId, result.value.name)
                        }

                        result
                    } catch (e: Exception) {
                        Result.Failure(
                            AppError.Internal(e.message ?: "Failed to create group")
                        )
                    }
                }
            }
        }
    }

    /**
     * Join a group using invite code
     */
    suspend fun joinGroup(
        request: JoinGroupRequest,
        userId: Int
    ): Result<GroupDto, AppError> {
        return when (val validation = GroupValidator.validateJoinGroup(request)) {
            is Result.Failure -> validation
            is Result.Success -> {
                withContext(Dispatchers.IO) {
                    try {
                        var username: String? = null

                        val result = transaction {
                            val group = groupRepository.findByInviteCode(request.inviteCode)
                                ?: return@transaction Result.Failure(
                                    AppError.InvalidInviteCode()
                                )

                            if (groupRepository.isMember(group.id, userId)) {
                                return@transaction Result.Failure(
                                    AppError.AlreadyMember()
                                )
                            }

                            groupRepository.addMember(group.id, userId)

                            val updatedGroup = groupRepository.findById(group.id)
                                ?: return@transaction Result.Failure(
                                    AppError.Internal("Failed to retrieve group")
                                )

                            val user = userRepository.findById(userId)
                            username = user?.username

                            Result.Success(updatedGroup.toDto())
                        }

                        when (result) {
                            is Result.Success -> {
                                activityService.logMemberJoined(result.value.id, userId, userId)

                                messageService.createSystemMessage(
                                    groupId = result.value.id,
                                    content = "${username ?: "Someone"} joined the group"
                                )

                                Result.Success(result.value)
                            }
                            is Result.Failure -> result
                        }
                    } catch (e: Exception) {
                        Result.Failure(
                            AppError.Internal(e.message ?: "Failed to join group")
                        )
                    }
                }
            }
        }
    }

    /**
     * Add a member to a group (owner only)
     */
    suspend fun addMember(
        groupId: Int,
        userIdToAdd: Int,
        requestingUserId: Int
    ): Result<GroupMemberDto, AppError> {
        return when (val validation = GroupValidator.validateAddMember(userIdToAdd)) {
            is Result.Failure -> validation
            is Result.Success -> {
                withContext(Dispatchers.IO) {
                    try {
                        val memberDto = transaction {
                            groupRepository.findById(groupId)
                                ?: return@transaction Result.Failure(AppError.NotFound("Group", groupId))

                            if (!groupRepository.isOwner(groupId, requestingUserId)) {
                                return@transaction Result.Failure(
                                    AppError.NotOwner(groupId)
                                )
                            }

                            val userToAdd = userRepository.findById(userIdToAdd)
                                ?: return@transaction Result.Failure(
                                    AppError.Validation("User to add does not exist")
                                )

                            if (groupRepository.isMember(groupId, userIdToAdd)) {
                                return@transaction Result.Failure(
                                    AppError.AlreadyMember()
                                )
                            }

                            groupRepository.addMember(groupId, userIdToAdd)

                            val group = groupRepository.findById(groupId)
                            val addedBy = userRepository.findById(requestingUserId)

                            Result.Success(
                                Triple(
                                    createGroupMemberDto(
                                        user = userToAdd,
                                        joinedAt = System.currentTimeMillis().toString(),
                                        isOwner = false
                                    ),
                                    group,
                                    addedBy
                                )
                            )
                        }

                        when (memberDto) {
                            is Result.Success -> {
                                val (member, group, addedBy) = memberDto.value

                                activityService.logMemberAdded(groupId, requestingUserId, userIdToAdd)

                                messageService.createSystemMessage(
                                    groupId = groupId,
                                    content = "${member.username} was added to the group"
                                )

                                launch(Dispatchers.IO) {
                                    if (group != null) {
                                        notificationService.notifyAddedToGroup(
                                            groupId = groupId,
                                            groupName = group.name,
                                            addedByUsername = addedBy?.username ?: "Someone",
                                            newMemberUserId = userIdToAdd
                                        )
                                    }
                                }

                                Result.Success(member)
                            }
                            is Result.Failure -> memberDto
                        }
                    } catch (e: Exception) {
                        Result.Failure(
                            AppError.Internal(e.message ?: "Failed to add member")
                        )
                    }
                }
            }
        }
    }

    /**
     * Remove a member from a group (owner only)
     */
    suspend fun removeMember(
        groupId: Int,
        userIdToRemove: Int,
        requestingUserId: Int
    ): Result<Unit, AppError> {
        return withContext(Dispatchers.IO) {
            try {
                var removedUsername: String? = null

                val removeResult = transaction {
                    if (!groupRepository.isMember(groupId, requestingUserId)) {
                        return@transaction Result.Failure(AppError.NotMember(groupId))
                    }

                    if (!groupRepository.isOwner(groupId, requestingUserId)) {
                        return@transaction Result.Failure(AppError.NotOwner(groupId))
                    }

                    if (userIdToRemove == requestingUserId) {
                        return@transaction Result.Failure(
                            AppError.Validation("Cannot remove yourself. Use leave group instead.")
                        )
                    }

                    if (groupRepository.isOwner(groupId, userIdToRemove)) {
                        return@transaction Result.Failure(
                            AppError.Validation("Cannot remove the group owner")
                        )
                    }

                    if (!groupRepository.isMember(groupId, userIdToRemove)) {
                        return@transaction Result.Failure(
                            AppError.NotFound("Member", userIdToRemove)
                        )
                    }

                    val user = userRepository.findById(userIdToRemove)
                    removedUsername = user?.username ?: "Unknown"

                    val balances = expenseRepository.calculateGroupBalances(groupId)
                    val userBalance = balances[userIdToRemove] ?: 0.0

                    if (userBalance < 0.0) {
                        debtHistoryRepository.create(
                            groupId = groupId,
                            userId = userIdToRemove,
                            amountOwed = -userBalance
                        )
                    }

                    groupRepository.removeMember(groupId, userIdToRemove)

                    Result.Success(Unit)
                }

                if (removeResult is Result.Success) {
                    activityService.logMemberRemoved(groupId, requestingUserId, userIdToRemove)

                    messageService.createSystemMessage(
                        groupId = groupId,
                        content = "${removedUsername ?: "A member"} was removed from the group"
                    )
                }

                removeResult
            } catch (e: Exception) {
                Result.Failure(
                    AppError.Internal(e.message ?: "Failed to remove member")
                )
            }
        }
    }

    /**
     * Get all groups for a user
     */
    suspend fun getUserGroups(userId: Int): Result<List<GroupDto>, AppError> {
        return withContext(Dispatchers.IO) {
            try {
                transaction {
                    val groups = groupRepository.findByUserId(userId)
                    Result.Success(groups.map { it.toDto() })
                }
            } catch (e: Exception) {
                Result.Failure(
                    AppError.Internal(e.message ?: "Failed to retrieve groups")
                )
            }
        }
    }

    /**
     * Get group by ID (only if user is member)
     */
    suspend fun getGroupById(
        groupId: Int,
        userId: Int
    ): Result<GroupDto, AppError> {
        return withContext(Dispatchers.IO) {
            try {
                transaction {
                    if (!groupRepository.isMember(groupId, userId)) {
                        return@transaction Result.Failure(AppError.NotMember(groupId))
                    }

                    val group = groupRepository.findById(groupId)
                        ?: return@transaction Result.Failure(AppError.NotFound("Group", groupId))

                    Result.Success(group.toDto())
                }
            } catch (e: Exception) {
                Result.Failure(
                    AppError.Internal(e.message ?: "Failed to retrieve group")
                )
            }
        }
    }

    suspend fun getGroupInviteDetails(
        groupId: Int,
        userId: Int
    ): Result<GroupInviteDetailsDto, AppError> {
        return withContext(Dispatchers.IO) {
            try {
                transaction {
                    if (!groupRepository.isMember(groupId, userId)) {
                        return@transaction Result.Failure(AppError.NotMember(groupId))
                    }

                    val group = groupRepository.findById(groupId)
                        ?: return@transaction Result.Failure(AppError.NotFound("Group", groupId))

                    Result.Success(
                        GroupInviteDetailsDto(
                            inviteCode = group.inviteCode,
                            deepLink = "japp://join/${group.inviteCode}", // Some hardcoded BS
                            groupId = group.id,
                            groupName = group.name,
                            memberCount = group.memberCount
                        )
                    )
                }
            } catch (e: Exception) {
                Result.Failure(
                    AppError.Internal(e.message ?: "Failed to get invite details")
                )
            }
        }
    }

    /**
     * Preview group details by invite code (accessible to non-members)
     */
    suspend fun previewGroupByInviteCode(
        inviteCode: String
    ): Result<GroupPreviewDto, AppError> {
        return when (val validation = GroupValidator.validatePreviewInviteCode(inviteCode)) {
            is Result.Failure -> validation
            is Result.Success -> {
                withContext(Dispatchers.IO) {
                    try {
                        transaction {
                            val group = groupRepository.findByInviteCode(inviteCode)
                                ?: return@transaction Result.Failure(
                                    AppError.InvalidInviteCode()
                                )

                            Result.Success(
                                GroupPreviewDto(
                                    id = group.id,
                                    name = group.name,
                                    description = group.description,
                                    memberCount = group.memberCount,
                                    createdAt = group.createdAt
                                )
                            )
                        }
                    } catch (e: Exception) {
                        Result.Failure(
                            AppError.Internal(e.message ?: "Failed to preview group")
                        )
                    }
                }
            }
        }
    }

    /**
     * Get members of a group (only if user is member)
     */
    suspend fun getGroupMembers(
        groupId: Int,
        userId: Int
    ): Result<List<GroupMemberDto>, AppError> {
        return withContext(Dispatchers.IO) {
            try {
                transaction {
                    if (!groupRepository.isMember(groupId, userId)) {
                        return@transaction Result.Failure(AppError.NotMember(groupId))
                    }

                    val group = groupRepository.findById(groupId)
                        ?: return@transaction Result.Failure(AppError.NotFound("Group", groupId))

                    val membersInfo = groupRepository.getMembersWithDetails(groupId)

                    val members = membersInfo.map { memberInfo ->
                        createGroupMemberDto(
                            user = memberInfo.user,
                            joinedAt = memberInfo.joinedAt,
                            isOwner = memberInfo.user.id == group.createdBy
                        )
                    }

                    Result.Success(members)
                }
            } catch (e: Exception) {
                Result.Failure(
                    AppError.Internal(e.message ?: "Failed to retrieve members")
                )
            }
        }
    }

    /**
     * Leave a group (owner transfers ownership automatically or deletes if last member)
     */
    suspend fun leaveGroup(
        groupId: Int,
        userId: Int
    ): Result<Unit, AppError> {
        return withContext(Dispatchers.IO) {
            try {
                var groupWasDeleted = false
                var systemMessageContent: String? = null

                val leaveResult = transaction {
                    if (!groupRepository.isMember(groupId, userId)) {
                        return@transaction Result.Failure(AppError.NotMember(groupId))
                    }

                    val group = groupRepository.findById(groupId)
                        ?: return@transaction Result.Failure(AppError.NotFound("Group", groupId))

                    val user = userRepository.findById(userId)
                    val username = user?.username ?: "Someone"

                    if (groupRepository.isOwner(groupId, userId)) {
                        if (group.memberCount == 1) {
                            groupRepository.delete(groupId)
                            groupWasDeleted = true
                            return@transaction Result.Success(Unit)
                        } else {
                            val members = groupRepository.getMembersSortedByJoinDate(groupId)
                            val nextOwner = members.firstOrNull { it != userId }
                                ?: return@transaction Result.Failure(
                                    AppError.Internal("No eligible member to transfer ownership")
                                )
                            groupRepository.transferOwnership(groupId, nextOwner)
                        }
                    }

                    val balances = expenseRepository.calculateGroupBalances(groupId)
                    val userBalance = balances[userId] ?: 0.0

                    if (userBalance < 0.0) {
                        debtHistoryRepository.create(
                            groupId = groupId,
                            userId = userId,
                            amountOwed = -userBalance
                        )
                    }

                    groupRepository.removeMember(groupId, userId)
                    systemMessageContent = "$username left the group"

                    Result.Success(Unit)
                }

                if (leaveResult is Result.Success) {
                    activityService.logMemberLeft(groupId, userId)

                    if (!groupWasDeleted && systemMessageContent != null) {
                        messageService.createSystemMessage(
                            groupId = groupId,
                            content = systemMessageContent
                        )
                    }
                }

                leaveResult
            } catch (e: Exception) {
                Result.Failure(
                    AppError.Internal(e.message ?: "Failed to leave group")
                )
            }
        }
    }

    /**
     * Delete a group (owner only, all balances must be zero)
     */
    suspend fun deleteGroup(
        groupId: Int,
        userId: Int
    ): Result<Unit, AppError> {
        return withContext(Dispatchers.IO) {
            try {
                transaction {
                    if (!groupRepository.isMember(groupId, userId)) {
                        return@transaction Result.Failure(AppError.NotMember(groupId))
                    }

                    if (!groupRepository.isOwner(groupId, userId)) {
                        return@transaction Result.Failure(AppError.NotOwner(groupId))
                    }

                    groupRepository.findById(groupId)
                        ?: return@transaction Result.Failure(AppError.NotFound("Group", groupId))

                    val balances = expenseRepository.calculateGroupBalances(groupId)
                    val hasOutstandingDebts = balances.values.any { it != 0.0 }

                    if (hasOutstandingDebts) {
                        return@transaction Result.Failure(
                            AppError.Validation("Cannot delete group with outstanding debts. All balances must be settled first.")
                        )
                    }

                    groupRepository.delete(groupId)
                    Result.Success(Unit)
                }
            } catch (e: Exception) {
                Result.Failure(
                    AppError.Internal(e.message ?: "Failed to delete group")
                )
            }
        }
    }

    /**
     * Get debt history for a group (traitors who left with unpaid debts)
     */
    suspend fun getGroupDebtHistory(
        groupId: Int,
        userId: Int
    ): Result<List<DebtHistoryDto>, AppError> {
        return withContext(Dispatchers.IO) {
            try {
                transaction {
                    if (!groupRepository.isMember(groupId, userId)) {
                        return@transaction Result.Failure(AppError.NotMember(groupId))
                    }

                    groupRepository.findById(groupId)
                        ?: return@transaction Result.Failure(AppError.NotFound("Group", groupId))

                    val debtRecords = debtHistoryRepository.findByGroupId(groupId)

                    val debtDtos = debtRecords.map { record ->
                        val user = userRepository.findById(record.userId)
                        createDebtHistoryDto(
                            debtHistory = record,
                            username = user?.username ?: "Unknown"
                        )
                    }

                    Result.Success(debtDtos)
                }
            } catch (e: Exception) {
                Result.Failure(
                    AppError.Internal(e.message ?: "Failed to retrieve debt history")
                )
            }
        }
    }

    /**
     * Check if user has notifications enabled for a specific group
     */
    suspend fun hasNotificationEnabled(
        groupId: Int,
        userId: Int
    ): Result<NotificationPreferenceDto, AppError> {
        return withContext(Dispatchers.IO) {
            try {
                transaction {
                    val notificationEnabled = groupRepository.hasNotificationEnabled(groupId, userId)

                    Result.Success(NotificationPreferenceDto(notificationEnabled))
                }
            } catch (e: Exception) {
                Result.Failure(
                    AppError.Internal(e.message ?: "Failed to retrieve notification setting!")
                )
            }
        }
    }

    /**
     * Set notification preference for a specific member of a group
     */
    suspend fun setNotificationEnabled(
        groupId: Int,
        userId: Int,
        enabled: Boolean
    ): Result<NotificationPreferenceDto, AppError> {
        return withContext(Dispatchers.IO) {
            try {
                transaction {
                    groupRepository.setNotificationEnabled(groupId, userId, enabled)

                    Result.Success(NotificationPreferenceDto(groupRepository.hasNotificationEnabled(groupId, userId)))
                }
            } catch (e: Exception) {
                Result.Failure(
                    AppError.Internal(e.message ?: "Failed to update notification setting!")
                )
            }
        }
    }
}