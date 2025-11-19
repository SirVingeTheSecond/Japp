package com.japp.models.domain

data class Attachment(
    val id: Int,
    val expenseId: Int,
    val uploadedBy: Int,
    val fileName: String,
    val storagePath: String,
    val fileSize: Long,
    val mimeType: String,
    val uploadedAt: String
)