package com.japp.api.responses.message

import com.japp.api.responses.MessageType

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