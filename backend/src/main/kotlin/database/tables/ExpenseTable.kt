package com.japp.database.tables

import org.jetbrains.exposed.v1.core.Table

object Expenses : Table("expenses") {
    val id = integer("id").autoIncrement()
    val groupId = integer("group_id").references(Groups.id)
    val paidBy = integer("paid_by").references(Users.id)
    val amount = double("amount")
    val currency = varchar("currency", 3).default("DKK")
    val description = text("description")
    val category = varchar("category", 50).nullable()
    val splitType = varchar("split_type", 20).default("equal")
    val createdAt = varchar("created_at", 255)
    val updatedAt = varchar("updated_at", 255)

    override val primaryKey = PrimaryKey(id)
}

object ExpenseSplits : Table("expense_splits") {
    val id = integer("id").autoIncrement()
    val expenseId = integer("expense_id").references(Expenses.id)
    val userId = integer("user_id").references(Users.id)
    val shareAmount = double("share_amount").nullable()
    val sharePercentage = double("share_percentage").nullable()

    override val primaryKey = PrimaryKey(id)
}