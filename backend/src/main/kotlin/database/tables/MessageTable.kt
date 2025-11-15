package com.japp.database.tables

import org.jetbrains.exposed.v1.core.Table

object Messages : Table("messages") {
    val id = integer("id").autoIncrement()
    val groupId = integer("group_id").references(Groups.id)
    val userId = integer("user_id").references(Users.id).nullable()
    val content = text("content")
    val messageType = varchar("message_type", 20).default("user")
    val createdAt = varchar("created_at", 255)
    val editedAt = varchar("edited_at", 255).nullable()
    val deletedAt = varchar("deleted_at", 255).nullable()

    override val primaryKey = PrimaryKey(id)
}

object MessageReadStatus : Table("message_read_status") {
    val messageId = integer("message_id").references(Messages.id)
    val userId = integer("user_id").references(Users.id)
    val readAt = varchar("read_at", 255)

    override val primaryKey = PrimaryKey(messageId, userId)
}