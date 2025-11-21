package com.japp.api.responses.attachment

data class AttachmentDto(
    val id: Int,
    val expenseId: Int,
    val uploadedBy: Int,
    val uploaderName: String,
    val fileName: String,
    val fileSize: Long,
    val mimeType: String,
    val uploadedAt: String,
    val downloadUrl: String
)

data class AttachmentListDto(
    val expenseId: Int,
    val attachments: List<AttachmentDto>
)