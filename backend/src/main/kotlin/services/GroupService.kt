package com.japp.services

import com.japp.models.*
import com.japp.models.dto.*
import com.japp.models.error.GroupError
import com.japp.repositories.interfaces.IGroupRepository
import com.japp.repositories.interfaces.IUserRepository
import com.japp.validation.GroupValidator
import com.japp.utils.toDto
import com.japp.utils.createGroupMemberDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class GroupService(
    private val groupRepository: IGroupRepository,
    private val userRepository: IUserRepository,
    private val activityService: ActivityService
) {

    /**
     * Create a new group
     */
    suspend fun createGroup(
        request: CreateGroupRequest,
        userId: Int
    ): Result<GroupDto, GroupError> {
        return when (val validation = GroupValidator.validateCreateGroup(request)) {
            is Result.Failure -> validation
            is Result.Success -> {
                withContext(Dispatchers.IO) {
                    try {
                        transaction {
                            userRepository.findById(userId)
                                ?: return@transaction Result.Failure(
                                    GroupError.InternalError("User not found")
                                )

                            val group = groupRepository.create(
                                name = request.name,
                                description = request.description,
                                createdBy = userId
                            )

                            activityService.logGroupCreated(group.id, userId, group.name)

                            Result.Success(group.toDto())
                        }
                    } catch (e: Exception) {
                        Result.Failure(
                            GroupError.InternalError(e.message ?: "Failed to create group")
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
    ): Result<GroupDto, GroupError> {
        return when (val validation = GroupValidator.validateJoinGroup(request)) {
            is Result.Failure -> validation
            is Result.Success -> {
                withContext(Dispatchers.IO) {
                    try {
                        transaction {
                            val group = groupRepository.findByInviteCode(request.inviteCode)
                                ?: return@transaction Result.Failure(
                                    GroupError.InvalidInviteCode()
                                )

                            if (groupRepository.isMember(group.id, userId)) {
                                return@transaction Result.Failure(
                                    GroupError.AlreadyMember()
                                )
                            }

                            groupRepository.addMember(group.id, userId)

                            activityService.logMemberJoined(group.id, userId, userId)

                            val updatedGroup = groupRepository.findById(group.id)
                                ?: return@transaction Result.Failure(
                                    GroupError.InternalError("Failed to retrieve group")
                                )

                            Result.Success(updatedGroup.toDto())
                        }
                    } catch (e: Exception) {
                        Result.Failure(
                            GroupError.InternalError(e.message ?: "Failed to join group")
                        )
                    }
                }
            }
        }
    }

    /**
     * Get all groups for a user
     */
    suspend fun getUserGroups(userId: Int): Result<List<GroupDto>, GroupError> {
        return withContext(Dispatchers.IO) {
            try {
                transaction {
                    val groups = groupRepository.findByUserId(userId)
                    Result.Success(groups.map { it.toDto() })
                }
            } catch (e: Exception) {
                Result.Failure(
                    GroupError.InternalError(e.message ?: "Failed to retrieve groups")
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
    ): Result<GroupDto, GroupError> {
        return withContext(Dispatchers.IO) {
            try {
                transaction {
                    if (!groupRepository.isMember(groupId, userId)) {
                        return@transaction Result.Failure(GroupError.NotMember(groupId))
                    }

                    val group = groupRepository.findById(groupId)
                        ?: return@transaction Result.Failure(GroupError.NotFound(groupId))

                    Result.Success(group.toDto())
                }
            } catch (e: Exception) {
                Result.Failure(
                    GroupError.InternalError(e.message ?: "Failed to retrieve group")
                )
            }
        }
    }

    /**
     * Get members of a group (only if user is member)
     */
    suspend fun getGroupMembers(
        groupId: Int,
        userId: Int
    ): Result<List<GroupMemberDto>, GroupError> {
        return withContext(Dispatchers.IO) {
            try {
                transaction {
                    if (!groupRepository.isMember(groupId, userId)) {
                        return@transaction Result.Failure(GroupError.NotMember(groupId))
                    }

                    val memberIds = groupRepository.getMembers(groupId)
                    val group = groupRepository.findById(groupId)
                        ?: return@transaction Result.Failure(GroupError.NotFound(groupId))

                    val members = memberIds.mapNotNull { memberId ->
                        userRepository.findById(memberId)?.let { user ->
                            createGroupMemberDto(
                                user = user,
                                joinedAt = "", // Track this separately if needed
                                isOwner = memberId == group.createdBy
                            )
                        }
                    }

                    Result.Success(members)
                }
            } catch (e: Exception) {
                Result.Failure(
                    GroupError.InternalError(e.message ?: "Failed to retrieve members")
                )
            }
        }
    }

    /**
     * Leave a group (cannot leave if owner)
     */
    suspend fun leaveGroup(
        groupId: Int,
        userId: Int
    ): Result<Unit, GroupError> {
        return withContext(Dispatchers.IO) {
            try {
                transaction {
                    if (!groupRepository.isMember(groupId, userId)) {
                        return@transaction Result.Failure(GroupError.NotMember(groupId))
                    }

                    if (groupRepository.isOwner(groupId, userId)) {
                        return@transaction Result.Failure(
                            GroupError.ValidationError("Group owner cannot leave. Delete the group instead.")
                        )
                    }

                    groupRepository.removeMember(groupId, userId)

                    activityService.logMemberLeft(groupId, userId)

                    Result.Success(Unit)
                }
            } catch (e: Exception) {
                Result.Failure(
                    GroupError.InternalError(e.message ?: "Failed to leave group")
                )
            }
        }
    }
}