package com.japp.database.tables

import org.jetbrains.exposed.v1.core.Table

object Attachments : Table("attachments") {
    val id = integer("id").autoIncrement()
    val expenseId = integer("expense_id").references(Expenses.id)
    val uploadedBy = integer("uploaded_by").references(Users.id)
    val fileName = varchar("file_name", 255)
    val storagePath = varchar("storage_path", 500)
    val fileSize = long("file_size")
    val mimeType = varchar("mime_type", 50)
    val uploadedAt = varchar("uploaded_at", 255)

    override val primaryKey = PrimaryKey(id)
}