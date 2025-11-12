package com.japp.database.tables

import org.jetbrains.exposed.v1.core.Table

object ActivityLogs : Table("activity_logs") {
    val id = integer("id").autoIncrement()
    val groupId = integer("group_id").references(Groups.id)
    val userId = integer("user_id").references(Users.id)
    val actionType = varchar("action_type", 50)
    val description = text("description")
    val relatedExpenseId = integer("related_expense_id").nullable()
    val relatedSettlementId = integer("related_settlement_id").nullable()
    val metadata = text("metadata").default("{}")
    val createdAt = varchar("created_at", 255)

    override val primaryKey = PrimaryKey(id)
}