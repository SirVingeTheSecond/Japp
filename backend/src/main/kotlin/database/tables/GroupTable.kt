package com.japp.database.tables

import org.jetbrains.exposed.v1.core.Table

object Groups : Table("groups") {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 255)
    val description = text("description").nullable()
    val inviteCode = varchar("invite_code", 20).uniqueIndex()
    val createdBy = integer("created_by").references(Users.id)
    val memberCount = integer("member_count").default(0)
    val totalExpenses = double("total_expenses").default(0.0)
    val createdAt = varchar("created_at", 255)
    val updatedAt = varchar("updated_at", 255)

    override val primaryKey = PrimaryKey(id)
}