package com.japp.models.domain

data class ActivityLog(
    val id: Int,
    val groupId: Int,
    val userId: Int,
    val actionType: String,
    val description: String,
    val relatedExpenseId: Int?,
    val relatedSettlementId: Int?,
    val metadata: String,
    val createdAt: String
)