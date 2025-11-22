package com.japp.services

import com.japp.models.*
import com.japp.models.domain.Message
import com.japp.models.dto.*
import com.japp.models.error.AppError
import com.japp.repositories.interfaces.IGroupRepository
import com.japp.repositories.interfaces.IMessageRepository
import com.japp.repositories.interfaces.IUserRepository
import com.japp.utils.toDto
import com.japp.validation.MessageValidator
import com.japp.websocket.WebSocketManager
import io.ktor.websocket.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class MessageService(
    private val messageRepository: IMessageRepository,
    private val groupRepository: IGroupRepository,
    private val userRepository: IUserRepository,
    private val webSocketManager: WebSocketManager
) {

    suspend fun createMessage(
        request: CreateMessageRequest,
        userId: Int
    ): Result<MessageDto, AppError> {
        return when (val validation = MessageValidator.validateCreateMessage(request)) {
            is Result.Failure -> validation
            is Result.Success -> {
                withContext(Dispatchers.IO) {
                    try {
                        val messageDto = transaction {
                            if (!groupRepository.isMember(request.groupId, userId)) {
                                return@transaction null
                            }

                            val message = messageRepository.create(
                                groupId = request.groupId,
                                userId = userId,
                                content = request.content,
                                messageType = MessageType.USER
                            )

                            toMessageDto(message)
                        }

                        if (messageDto == null) {
                            return@withContext Result.Failure(
                                AppError.NotMember(request.groupId)
                            )
                        }

                        // Exclude sender from broadcast since they receive MESSAGE_SENT acknowledge
                        // ToDo: For multi-device support, we should only exclude the sending session, not all user sessions
                        webSocketManager.broadcastToGroup(
                            groupId = request.groupId,
                            message = WebSocketMessage(
                                type = WebSocketMessageType.NEW_MESSAGE,
                                groupId = request.groupId,
                                userId = userId,
                                message = messageDto
                            ),
                            excludeUserId = userId
                        )

                        Result.Success(messageDto)
                    } catch (e: Exception) {
                        Result.Failure(
                            AppError.Internal(e.message ?: "Failed to create message")
                        )
                    }
                }
            }
        }
    }

    suspend fun createSystemMessage(
        groupId: Int,
        content: String
    ): Result<MessageDto, AppError> {
        return withContext(Dispatchers.IO) {
            try {
                val messageDto = transaction {
                    val message = messageRepository.create(
                        groupId = groupId,
                        userId = null,
                        content = content,
                        messageType = MessageType.SYSTEM
                    )

                    toMessageDto(message)
                }

                webSocketManager.broadcastToGroup(
                    groupId = groupId,
                    message = WebSocketMessage(
                        type = WebSocketMessageType.NEW_MESSAGE,
                        groupId = groupId,
                        message = messageDto
                    )
                )

                Result.Success(messageDto)
            } catch (e: Exception) {
                Result.Failure(
                    AppError.Internal(e.message ?: "Failed to create system message")
                )
            }
        }
    }

    suspend fun getMessages(
        groupId: Int,
        userId: Int,
        limit: Int = 50,
        beforeCursor: String? = null
    ): Result<MessagePageDto, AppError> {
        return withContext(Dispatchers.IO) {
            try {
                transaction {
                    if (!groupRepository.isMember(groupId, userId)) {
                        return@transaction Result.Failure(AppError.NotMember(groupId))
                    }

                    val messages = messageRepository.findByGroupId(
                        groupId = groupId,
                        limit = limit + 1,
                        beforeTimestamp = beforeCursor
                    )

                    val hasMore = messages.size > limit
                    val messagesToReturn = if (hasMore) messages.dropLast(1) else messages

                    val messageIds = messagesToReturn.map { it.id }
                    val readStatusMap = messageRepository.getReadStatusForMessages(messageIds)

                    val messageDtos = messagesToReturn.map { message ->
                        val readByUserIds = readStatusMap[message.id]?.map { it.userId } ?: emptyList()
                        toMessageDto(message, readByUserIds)
                    }

                    val nextCursor = if (hasMore) messagesToReturn.lastOrNull()?.createdAt else null

                    Result.Success(
                        MessagePageDto(
                            messages = messageDtos,
                            hasMore = hasMore,
                            nextCursor = nextCursor
                        )
                    )
                }
            } catch (e: Exception) {
                Result.Failure(
                    AppError.Internal(e.message ?: "Failed to retrieve messages")
                )
            }
        }
    }

    suspend fun markMessagesAsRead(
        messageIds: List<Int>,
        userId: Int,
        groupId: Int
    ): Result<Unit, AppError> {
        return withContext(Dispatchers.IO) {
            try {
                val isMember = transaction {
                    if (!groupRepository.isMember(groupId, userId)) {
                        return@transaction false
                    }

                    messageRepository.markMultipleAsRead(messageIds, userId)

                    true
                }

                if (!isMember) {
                    return@withContext Result.Failure(AppError.NotMember(groupId))
                }

                webSocketManager.broadcastToGroup(
                    groupId = groupId,
                    message = WebSocketMessage(
                        type = WebSocketMessageType.MESSAGE_READ,
                        groupId = groupId,
                        userId = userId,
                        messageIds = messageIds
                    )
                )

                Result.Success(Unit)
            } catch (e: Exception) {
                Result.Failure(
                    AppError.Internal(e.message ?: "Failed to mark messages as read")
                )
            }
        }
    }

    suspend fun deleteMessage(
        messageId: Int,
        userId: Int
    ): Result<Unit, AppError> {
        return withContext(Dispatchers.IO) {
            try {
                val deletionResult = transaction {
                    val message = messageRepository.findById(messageId)
                        ?: return@transaction Result.Failure(AppError.NotFound("Message", messageId))

                    if (!groupRepository.isMember(message.groupId, userId)) {
                        return@transaction Result.Failure(
                            AppError.NotMember(message.groupId)
                        )
                    }

                    if (message.userId != userId) {
                        return@transaction Result.Failure(
                            AppError.Unauthorized("You can only delete your own messages")
                        )
                    }

                    if (message.messageType == MessageType.SYSTEM) {
                        return@transaction Result.Failure(
                            AppError.Unauthorized("System messages cannot be deleted")
                        )
                    }

                    messageRepository.softDelete(messageId)

                    Result.Success(message.groupId)
                }

                when (deletionResult) {
                    is Result.Failure -> deletionResult
                    is Result.Success -> {
                        webSocketManager.broadcastToGroup(
                            groupId = deletionResult.value,
                            message = WebSocketMessage(
                                type = WebSocketMessageType.MESSAGE_DELETED,
                                groupId = deletionResult.value,
                                userId = userId,
                                messageIds = listOf(messageId)
                            )
                        )
                        Result.Success(Unit)
                    }
                }
            } catch (e: Exception) {
                Result.Failure(
                    AppError.Internal(e.message ?: "Failed to delete message")
                )
            }
        }
    }

    /**
     * Subscribe user's WebSocket session to a group's message channel
     */
    suspend fun subscribeToGroup(
        groupId: Int,
        userId: Int,
        session: WebSocketSession
    ): Result<Unit, AppError> {
        println("subscribeToGroup called: groupId=$groupId, userId=$userId")

        return withContext(Dispatchers.IO) {
            try {
                val isMember = transaction {
                    groupRepository.isMember(groupId, userId)
                }

                println("User $userId is member of group $groupId: $isMember")

                if (!isMember) {
                    println("User $userId NOT a member of group $groupId")
                    return@withContext Result.Failure(AppError.NotMember(groupId))
                }

                val subscribed = webSocketManager.subscribeToGroup(session, groupId)
                println("Subscription result: $subscribed")

                if (subscribed) {
                    Result.Success(Unit)
                } else {
                    Result.Failure(AppError.Internal("Failed to subscribe to group"))
                }
            } catch (e: Exception) {
                println("ERROR in subscribeToGroup: ${e.message}")
                e.printStackTrace()
                Result.Failure(AppError.Internal(e.message ?: "Subscription failed"))
            }
        }
    }

    /**
     * Unsubscribe user's WebSocket session from a group's message channel
     */
    suspend fun unsubscribeFromGroup(
        groupId: Int,
        session: WebSocketSession
    ): Result<Unit, AppError> {
        return withContext(Dispatchers.IO) {
            try {
                webSocketManager.unsubscribeFromGroup(session, groupId)
                Result.Success(Unit)
            } catch (e: Exception) {
                Result.Failure(
                    AppError.Internal(e.message ?: "Failed to unsubscribe from group")
                )
            }
        }
    }

    private fun toMessageDto(message: Message, readByUserIds: List<Int> = emptyList()): MessageDto {
        val userName = message.userId?.let { userId ->
            userRepository.findById(userId)?.username
        }
        return message.toDto(userName, readByUserIds)
    }
}