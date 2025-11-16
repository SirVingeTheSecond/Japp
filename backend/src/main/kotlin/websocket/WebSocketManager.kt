package com.japp.websocket

import com.japp.models.dto.WebSocketMessage
import io.ktor.websocket.*
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

class WebSocketManager {

    private val groupConnections = ConcurrentHashMap<Int, MutableSet<WebSocketSessionWrapper>>()
    private val userSessions = ConcurrentHashMap<Int, MutableSet<WebSocketSessionWrapper>>()

    fun registerConnection(
        groupId: Int,
        userId: Int,
        session: WebSocketSession
    ) {
        val wrapper = WebSocketSessionWrapper(session, userId, groupId)

        groupConnections.compute(groupId) { _, existing ->
            (existing ?: mutableSetOf()).apply { add(wrapper) }
        }

        userSessions.compute(userId) { _, existing ->
            (existing ?: mutableSetOf()).apply { add(wrapper) }
        }
    }

    fun unregisterConnection(
        groupId: Int,
        userId: Int,
        session: WebSocketSession
    ) {
        groupConnections.computeIfPresent(groupId) { _, sessions ->
            sessions.apply { removeIf { it.session == session } }
                .takeIf { it.isNotEmpty() }
        }

        userSessions.computeIfPresent(userId) { _, sessions ->
            sessions.apply { removeIf { it.session == session } }
                .takeIf { it.isNotEmpty() }
        }
    }

    suspend fun broadcastToGroup(
        groupId: Int,
        message: WebSocketMessage,
        excludeUserId: Int? = null
    ) {
        val sessions = groupConnections[groupId] ?: return
        val json = Json.encodeToString(message)

        sessions.forEach { wrapper ->
            if (excludeUserId == null || wrapper.userId != excludeUserId) {
                try {
                    wrapper.session.send(Frame.Text(json))
                } catch (_: Exception) {
                    // Connection closed, will be cleaned up on unregister
                }
            }
        }
    }

    suspend fun sendToUser(
        userId: Int,
        message: WebSocketMessage
    ) {
        val sessions = userSessions[userId] ?: return
        val json = Json.encodeToString(message)

        sessions.forEach { wrapper ->
            try {
                wrapper.session.send(Frame.Text(json))
            } catch (_: Exception) {
                // Connection closed, will be cleaned up on unregister
            }
        }
    }

    fun getGroupConnectionCount(groupId: Int): Int {
        return groupConnections[groupId]?.size ?: 0
    }

    fun getActiveUsersInGroup(groupId: Int): Set<Int> {
        return groupConnections[groupId]?.mapTo(mutableSetOf()) { it.userId } ?: emptySet()
    }
}

data class WebSocketSessionWrapper(
    val session: WebSocketSession,
    val userId: Int,
    val groupId: Int
)