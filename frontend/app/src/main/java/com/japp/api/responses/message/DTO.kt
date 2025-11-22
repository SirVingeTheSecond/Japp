package com.japp.api.responses.message

import com.japp.api.responses.MessageType
import com.japp.api.responses.WebSocketMessageType

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

data class MessagePageDto(
    val messages: List<MessageDto>,
    val hasMore: Boolean,
    val nextCursor: String?
)

data class WebSocketMessageDto(
    val type: WebSocketMessageType,
    val groupId: Int? = null,
    val userId: Int? = null,
    val username: String? = null,
    val message: MessageDto? = null,
    val messageIds: List<Int>? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val error: String? = null
)