package com.japp.repositories.implementations

import com.japp.database.tables.MessageReadStatus as MessageReadStatusTable
import com.japp.database.tables.Messages
import com.japp.models.MessageType
import com.japp.models.domain.Message
import com.japp.models.domain.MessageReadStatus
import com.japp.repositories.interfaces.IMessageRepository
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class MessageRepository : IMessageRepository {

    init {
        transaction {
            SchemaUtils.create(Messages, MessageReadStatusTable)
        }
    }

    override fun create(
        groupId: Int,
        userId: Int?,
        content: String,
        messageType: MessageType
    ): Message {
        val timestamp = System.currentTimeMillis().toString()

        val messageId = Messages.insert {
            it[Messages.groupId] = groupId
            it[Messages.userId] = userId
            it[Messages.content] = content
            it[Messages.messageType] = messageType.value
            it[createdAt] = timestamp
            it[editedAt] = null
            it[deletedAt] = null
        }[Messages.id]

        return findById(messageId)!!
    }

    override fun findById(messageId: Int): Message? {
        return Messages.selectAll()
            .where { Messages.id eq messageId }
            .map { rowToMessage(it) }
            .singleOrNull()
    }

    override fun findByGroupId(
        groupId: Int,
        limit: Int,
        beforeTimestamp: String?
    ): List<Message> {
        var query = Messages.selectAll()
            .where { Messages.groupId eq groupId }

        beforeTimestamp?.let {
            query = query.andWhere { Messages.createdAt less it }
        }

        return query
            .orderBy(Messages.createdAt to SortOrder.DESC)
            .limit(limit)
            .map { rowToMessage(it) }
    }

    override fun softDelete(messageId: Int): Boolean {
        val timestamp = System.currentTimeMillis().toString()
        val updated = Messages.update({ Messages.id eq messageId }) {
            it[deletedAt] = timestamp
        }
        return updated > 0
    }

    override fun markAsRead(messageId: Int, userId: Int): MessageReadStatus {
        val timestamp = System.currentTimeMillis().toString()

        MessageReadStatusTable.insertIgnore {
            it[MessageReadStatusTable.messageId] = messageId
            it[MessageReadStatusTable.userId] = userId
            it[readAt] = timestamp
        }

        return MessageReadStatusTable.selectAll()
            .where {
                (MessageReadStatusTable.messageId eq messageId) and
                        (MessageReadStatusTable.userId eq userId)
            }
            .map { rowToMessageReadStatus(it) }
            .single()
    }

    override fun markMultipleAsRead(messageIds: List<Int>, userId: Int): List<MessageReadStatus> {
        if (messageIds.isEmpty()) return emptyList()

        val timestamp = System.currentTimeMillis().toString()

        MessageReadStatusTable.batchInsert(
            messageIds,
            ignore = true
        ) { messageId ->
            this[MessageReadStatusTable.messageId] = messageId
            this[MessageReadStatusTable.userId] = userId
            this[MessageReadStatusTable.readAt] = timestamp
        }

        return MessageReadStatusTable.selectAll()
            .where {
                (MessageReadStatusTable.messageId inList messageIds) and
                        (MessageReadStatusTable.userId eq userId)
            }
            .map { rowToMessageReadStatus(it) }
    }

    override fun getReadStatus(messageId: Int): List<MessageReadStatus> {
        return MessageReadStatusTable.selectAll()
            .where { MessageReadStatusTable.messageId eq messageId }
            .map { rowToMessageReadStatus(it) }
    }

    override fun getReadStatusForMessages(messageIds: List<Int>): Map<Int, List<MessageReadStatus>> {
        if (messageIds.isEmpty()) return emptyMap()

        val readStatuses = MessageReadStatusTable.selectAll()
            .where { MessageReadStatusTable.messageId inList messageIds }
            .map { rowToMessageReadStatus(it) }

        return readStatuses.groupBy { it.messageId }
    }

    private fun rowToMessage(row: ResultRow) = Message(
        id = row[Messages.id],
        groupId = row[Messages.groupId],
        userId = row[Messages.userId],
        content = row[Messages.content],
        messageType = MessageType.fromString(row[Messages.messageType]) ?: MessageType.USER,
        createdAt = row[Messages.createdAt],
        editedAt = row[Messages.editedAt],
        deletedAt = row[Messages.deletedAt]
    )

    private fun rowToMessageReadStatus(row: ResultRow) = MessageReadStatus(
        messageId = row[MessageReadStatusTable.messageId],
        userId = row[MessageReadStatusTable.userId],
        readAt = row[MessageReadStatusTable.readAt]
    )
}