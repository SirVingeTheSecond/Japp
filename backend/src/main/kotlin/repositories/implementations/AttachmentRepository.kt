package com.japp.repositories.implementations

import com.japp.database.tables.Attachments
import com.japp.models.domain.Attachment
import com.japp.services.interfaces.IAttachmentRepository
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class AttachmentRepository : IAttachmentRepository {

    init {
        transaction {
            SchemaUtils.create(Attachments)
        }
    }

    override fun create(
        expenseId: Int,
        uploadedBy: Int,
        fileName: String,
        storagePath: String,
        fileSize: Long,
        mimeType: String
    ): Attachment {
        val timestamp = System.currentTimeMillis().toString()

        val attachmentId = Attachments.insert {
            it[Attachments.expenseId] = expenseId
            it[Attachments.uploadedBy] = uploadedBy
            it[Attachments.fileName] = fileName
            it[Attachments.storagePath] = storagePath
            it[Attachments.fileSize] = fileSize
            it[Attachments.mimeType] = mimeType
            it[uploadedAt] = timestamp
        }[Attachments.id]

        return findById(attachmentId)!!
    }

    override fun findById(attachmentId: Int): Attachment? {
        return Attachments.selectAll()
            .where { Attachments.id eq attachmentId }
            .map { rowToAttachment(it) }
            .singleOrNull()
    }

    override fun findByExpenseId(expenseId: Int): List<Attachment> {
        return Attachments.selectAll()
            .where { Attachments.expenseId eq expenseId }
            .orderBy(Attachments.uploadedAt to SortOrder.DESC)
            .map { rowToAttachment(it) }
    }

    override fun delete(attachmentId: Int): Boolean {
        val deleted = Attachments.deleteWhere { Attachments.id eq attachmentId }
        return deleted > 0
    }

    private fun rowToAttachment(row: ResultRow) = Attachment(
        id = row[Attachments.id],
        expenseId = row[Attachments.expenseId],
        uploadedBy = row[Attachments.uploadedBy],
        fileName = row[Attachments.fileName],
        storagePath = row[Attachments.storagePath],
        fileSize = row[Attachments.fileSize],
        mimeType = row[Attachments.mimeType],
        uploadedAt = row[Attachments.uploadedAt]
    )
}