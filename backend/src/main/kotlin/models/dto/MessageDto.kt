package com.japp.models.dto

import com.japp.models.MessageType
import com.japp.models.WebSocketMessageType
import kotlinx.serialization.Serializable

@Serializable
data class CreateMessageRequest(
    val groupId: Int,
    val content: String
)

@Serializable
data class MessageDto(
    val id: Int,
    val groupId: Int,
    val userId: Int?,
    val userName: String?,
    val content: String,
    val messageType: MessageType,
    val createdAt: String,
    val editedAt: String?,
    val isDeleted: Boolean,
    val readBy: List<Int> = emptyList()
)

@Serializable
data class MessagePageDto(
    val messages: List<MessageDto>,
    val hasMore: Boolean,
    val nextCursor: String?
)

@Serializable
data class MarkMessageReadRequest(
    val messageIds: List<Int>
)

@Serializable
data class WebSocketMessage(
    val type: WebSocketMessageType,
    val groupId: Int? = null,
    val userId: Int? = null,
    val username: String? = null,
    val message: MessageDto? = null,
    val content: String? = null,
    val messageIds: List<Int>? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val error: String? = null
)