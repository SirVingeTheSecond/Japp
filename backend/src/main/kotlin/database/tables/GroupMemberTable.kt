package com.japp.database.tables

import org.jetbrains.exposed.v1.core.Table

object GroupMembers : Table("group_members") {
    val groupId = integer("group_id").references(Groups.id)
    val userId = integer("user_id").references(Users.id)
    val joinedAt = varchar("joined_at", 255)
    val notificationEnabled = bool("notification_enabled").default(true)

    override val primaryKey = PrimaryKey(groupId, userId)
}

object DebtHistory : Table("debt_history") {
    val id = integer("id").autoIncrement()
    val groupId = integer("group_id").references(Groups.id)
    val userId = integer("user_id").references(Users.id)
    val amountOwed = double("amount_owed")
    val leftAt = varchar("left_at", 255)

    override val primaryKey = PrimaryKey(id)
}
