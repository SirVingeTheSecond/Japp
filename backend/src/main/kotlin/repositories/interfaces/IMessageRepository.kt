package com.japp.repositories.interfaces

import com.japp.models.MessageType
import com.japp.models.domain.Message
import com.japp.models.domain.MessageReadStatus

interface IMessageRepository {
    fun create(
        groupId: Int,
        userId: Int?,
        content: String,
        messageType: MessageType
    ): Message

    fun findById(messageId: Int): Message?

    fun findByGroupId(
        groupId: Int,
        limit: Int,
        beforeTimestamp: String? = null
    ): List<Message>

    fun softDelete(messageId: Int): Boolean

    fun markAsRead(messageId: Int, userId: Int): MessageReadStatus

    fun markMultipleAsRead(messageIds: List<Int>, userId: Int): List<MessageReadStatus>

    fun getReadStatus(messageId: Int): List<MessageReadStatus>

    fun getReadStatusForMessages(messageIds: List<Int>): Map<Int, List<MessageReadStatus>>
}