package com.japp.database.tables

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table

object ActivityLogs : Table("activity_logs") {
    val id = integer("id").autoIncrement()
    val groupId = integer("group_id").references(Groups.id, onDelete = ReferenceOption.CASCADE)
    val userId = integer("user_id").references(Users.id, onDelete = ReferenceOption.CASCADE)
    val actionType = varchar("action_type", 50)
    val description = text("description")
    val relatedExpenseId = integer("related_expense_id")
        .references(Expenses.id, onDelete = ReferenceOption.SET_NULL)
        .nullable()
    val relatedSettlementId = integer("related_settlement_id")
        .references(Settlements.id, onDelete = ReferenceOption.SET_NULL)
        .nullable()
    val metadata = text("metadata").default("{}")
    val createdAt = varchar("created_at", 255)

    override val primaryKey = PrimaryKey(id)
}