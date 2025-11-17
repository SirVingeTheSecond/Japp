package com.japp.database.tables

import org.jetbrains.exposed.v1.core.Table

object Settlements : Table("settlements") {
    val id = integer("id").autoIncrement()
    val groupId = integer("group_id").references(Groups.id)
    val fromUserId = integer("from_user_id").references(Users.id)
    val toUserId = integer("to_user_id").references(Users.id)
    val amount = double("amount")
    val status = varchar("status", 20).default("pending")
    val createdAt = varchar("created_at", 255)
    val completedAt = varchar("completed_at", 255).nullable()

    override val primaryKey = PrimaryKey(id)
}