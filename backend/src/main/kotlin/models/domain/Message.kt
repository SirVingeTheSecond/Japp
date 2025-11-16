package com.japp.models.domain

import com.japp.models.MessageType

data class Message(
    val id: Int,
    val groupId: Int,
    val userId: Int?,
    val content: String,
    val messageType: MessageType,
    val createdAt: String,
    val editedAt: String?,
    val deletedAt: String?
)

data class MessageReadStatus(
    val messageId: Int,
    val userId: Int,
    val readAt: String
)