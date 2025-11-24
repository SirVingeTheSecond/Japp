package com.japp.services

import com.japp.models.Result
import com.japp.models.WebSocketMessageType
import com.japp.models.dto.CreateMessageRequest
import com.japp.models.dto.WebSocketMessage
import com.japp.services.interfaces.IUserRepository
import com.japp.utils.toWebSocketMessage
import com.japp.websocket.WebSocketManager
import io.ktor.websocket.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class ChatWebSocketService(
    private val messageService: MessageService,
    private val userRepository: IUserRepository,
    private val webSocketManager: WebSocketManager
) {

    fun handleConnect(userId: Int, session: WebSocketSession): WebSocketMessage {
        webSocketManager.registerSession(userId, session)
        return WebSocketMessage(
            type = WebSocketMessageType.CONNECTED,
            userId = userId
        )
    }

    suspend fun handleMessage(
        message: WebSocketMessage,
        userId: Int,
        session: WebSocketSession
    ): WebSocketMessage? {
        return when (message.type) {
            WebSocketMessageType.SUBSCRIBE -> handleSubscribe(message, userId, session)
            WebSocketMessageType.UNSUBSCRIBE -> handleUnsubscribe(message, session)
            WebSocketMessageType.TYPING_START, WebSocketMessageType.TYPING_STOP -> {
                handleTypingIndicator(message, userId, session)
                null
            }
            WebSocketMessageType.PING -> WebSocketMessage(type = WebSocketMessageType.PONG)
            WebSocketMessageType.NEW_MESSAGE -> handleNewMessage(message, userId)
            else -> null
        }
    }

    fun handleDisconnect(session: WebSocketSession) {
        webSocketManager.unregisterSession(session)
    }

    suspend fun startHeartbeat(session: WebSocketSession, userId: Int): Job {
        return webSocketManager.startHeartbeat(session, userId)
    }

    private suspend fun handleSubscribe(
        message: WebSocketMessage,
        userId: Int,
        session: WebSocketSession
    ): WebSocketMessage {
        val groupId = message.groupId
        if (groupId == null) {
            return WebSocketMessage(
                type = WebSocketMessageType.ERROR,
                error = "groupId required for subscribe"
            )
        }

        return when (val result = messageService.subscribeToGroup(groupId, userId, session)) {
            is Result.Success -> WebSocketMessage(
                type = WebSocketMessageType.SUBSCRIBED,
                groupId = groupId
            )
            is Result.Failure -> WebSocketMessage(
                type = WebSocketMessageType.ERROR,
                groupId = groupId,
                error = result.error.message
            )
        }
    }

    private suspend fun handleUnsubscribe(
        message: WebSocketMessage,
        session: WebSocketSession
    ): WebSocketMessage {
        val groupId = message.groupId ?: return WebSocketMessage(
            type = WebSocketMessageType.ERROR,
            error = "groupId required for unsubscribe"
        )

        messageService.unsubscribeFromGroup(groupId, session)
        return WebSocketMessage(
            type = WebSocketMessageType.UNSUBSCRIBED,
            groupId = groupId
        )
    }

    private suspend fun handleTypingIndicator(
        message: WebSocketMessage,
        userId: Int,
        session: WebSocketSession
    ) {
        val groupId = message.groupId ?: return

        if (webSocketManager.isSubscribed(session, groupId)) {
            val username = withContext(Dispatchers.IO) {
                transaction {
                    userRepository.findById(userId)?.username
                }
            } ?: "Unknown"

            // Typing indicator broadcasted to all group members except sender
            webSocketManager.broadcastToGroup(
                groupId = groupId,
                message = message.copy(
                    userId = userId,
                    username = username
                ),
                excludeUserId = userId
            )
        }
    }

    private suspend fun handleNewMessage(
        message: WebSocketMessage,
        userId: Int
    ): WebSocketMessage {
        val groupId = message.groupId
        val content = message.content ?: message.message?.content  // Accept both for backward compat

        // a bit of validation, innit?
        if (groupId == null || content.isNullOrBlank()) {
            return WebSocketMessage(
                type = WebSocketMessageType.ERROR,
                error = "groupId and content are required"
            )
        }

        // Delegate this bad boy to MessageService
        val request = CreateMessageRequest(
            groupId = groupId,
            content = content
        )

        val result = messageService.createMessage(request, userId)
        return result.toWebSocketMessage()
    }
}
