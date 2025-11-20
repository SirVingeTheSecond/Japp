package com.japp.websocket

import com.japp.models.WebSocketMessageType
import com.japp.models.dto.WebSocketMessage
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class WebSocketManager(
    private val heartbeatInterval: Duration = 20.seconds
) {
    private val logger = LoggerFactory.getLogger(WebSocketManager::class.java)

    private val sessions = ConcurrentHashMap<WebSocketSession, SessionInfo>()
    private val groupSubscriptions = ConcurrentHashMap<Int, MutableSet<WebSocketSession>>()

    data class SessionInfo(
        val userId: Int,
        val subscribedGroups: MutableSet<Int> = mutableSetOf()
    )

    fun registerSession(userId: Int, session: WebSocketSession) {
        sessions[session] = SessionInfo(userId)
        logger.info("WebSocket session registered - User: $userId")
    }

    fun subscribeToGroup(session: WebSocketSession, groupId: Int): Boolean {
        val sessionInfo = sessions[session]
        if (sessionInfo == null) {
            logger.warn("Attempted to subscribe unregistered session to group $groupId")
            return false
        }

        sessionInfo.subscribedGroups.add(groupId)

        groupSubscriptions.compute(groupId) { _, subscribers ->
            (subscribers ?: mutableSetOf()).apply { add(session) }
        }

        logger.info("User ${sessionInfo.userId} subscribed to group $groupId")
        return true
    }

    fun unsubscribeFromGroup(session: WebSocketSession, groupId: Int): Boolean {
        val sessionInfo = sessions[session] ?: return false

        val wasSubscribed = sessionInfo.subscribedGroups.remove(groupId)

        groupSubscriptions.computeIfPresent(groupId) { _, subscribers ->
            subscribers.apply { remove(session) }
                .takeIf { it.isNotEmpty() }
        }

        if (wasSubscribed) {
            logger.info("User ${sessionInfo.userId} unsubscribed from group $groupId")
        }
        return wasSubscribed
    }

    fun unregisterSession(session: WebSocketSession) {
        val sessionInfo = sessions.remove(session)
        if (sessionInfo != null) {
            sessionInfo.subscribedGroups.forEach { groupId ->
                groupSubscriptions.computeIfPresent(groupId) { _, subscribers ->
                    subscribers.apply { remove(session) }
                        .takeIf { it.isNotEmpty() }
                }
            }
            logger.info("WebSocket session unregistered - User: ${sessionInfo.userId}")
        }
    }

    suspend fun startHeartbeat(
        session: WebSocketSession,
        userId: Int
    ): Job = coroutineScope {
        launch {
            try {
                while (isActive) {
                    delay(heartbeatInterval)

                    try {
                        val pingMessage = Json.encodeToString(
                            WebSocketMessage.serializer(),
                            WebSocketMessage(type = WebSocketMessageType.PING)
                        )
                        session.send(Frame.Text(pingMessage))
                        logger.debug("Heartbeat ping sent - User: $userId")

                    } catch (e: Exception) {
                        logger.warn("Heartbeat failed for User: $userId - ${e.message}")
                        session.close(CloseReason(CloseReason.Codes.INTERNAL_ERROR, "Heartbeat failed"))
                        break
                    }
                }
            } catch (_: CancellationException) {
                logger.debug("Heartbeat cancelled for User: $userId")
            } finally {
                unregisterSession(session)
            }
        }
    }

    suspend fun broadcastToGroup(
        groupId: Int,
        message: WebSocketMessage,
        excludeUserId: Int? = null
    ) {
        val subscribers = groupSubscriptions[groupId] ?: return
        val json = Json.encodeToString(WebSocketMessage.serializer(), message)

        logger.debug("Broadcasting to group $groupId (${subscribers.size} subscribers)")

        subscribers.forEach { session ->
            val sessionInfo = sessions[session]
            if (sessionInfo != null && (excludeUserId == null || sessionInfo.userId != excludeUserId)) {
                try {
                    session.send(Frame.Text(json))
                } catch (e: Exception) {
                    logger.warn("Failed to send to User: ${sessionInfo.userId} - ${e.message}")
                }
            }
        }
    }

    suspend fun sendToUser(
        userId: Int,
        message: WebSocketMessage
    ) {
        val json = Json.encodeToString(WebSocketMessage.serializer(), message)
        val userSessions = sessions.filterValues { it.userId == userId }.keys

        logger.debug("Sending to user $userId (${userSessions.size} sessions)")

        userSessions.forEach { session ->
            try {
                session.send(Frame.Text(json))
            } catch (e: Exception) {
                logger.warn("Failed to send to User: $userId - ${e.message}")
            }
        }
    }

    fun getGroupConnectionCount(groupId: Int): Int {
        return groupSubscriptions[groupId]?.size ?: 0
    }

    fun getActiveUsersInGroup(groupId: Int): Set<Int> {
        val subscribers = groupSubscriptions[groupId] ?: return emptySet()
        return subscribers.mapNotNull { sessions[it]?.userId }.toSet()
    }

    fun getSubscribedGroups(session: WebSocketSession): Set<Int> {
        return sessions[session]?.subscribedGroups?.toSet() ?: emptySet()
    }

    fun isSubscribed(session: WebSocketSession, groupId: Int): Boolean {
        return sessions[session]?.subscribedGroups?.contains(groupId) ?: false
    }
}