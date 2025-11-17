package com.japp.models.domain

import com.japp.models.ActivityType

data class ActivityLog(
    val id: Int,
    val groupId: Int,
    val userId: Int,
    val actionType: ActivityType,
    val description: String,
    val relatedExpenseId: Int?,
    val relatedSettlementId: Int?,
    val metadata: String,
    val createdAt: String
)