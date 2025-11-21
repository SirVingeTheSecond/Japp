package com.japp.repositories.interfaces

import com.japp.models.domain.Attachment

interface IAttachmentRepository {
    fun create(
        expenseId: Int,
        uploadedBy: Int,
        fileName: String,
        storagePath: String,
        fileSize: Long,
        mimeType: String
    ): Attachment

    fun findById(attachmentId: Int): Attachment?
    fun findByExpenseId(expenseId: Int): List<Attachment>
    fun delete(attachmentId: Int): Boolean
}