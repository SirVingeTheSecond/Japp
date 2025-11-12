package com.japp.models.dto

import kotlinx.serialization.Serializable

@Serializable
data class ActivityDto(
    val id: Int,
    val groupId: Int,
    val userId: Int,
    val userName: String,
    val actionType: String,
    val description: String,
    val relatedExpenseId: Int?,
    val relatedSettlementId: Int?,
    val metadata: Map<String, String>,
    val createdAt: String
)

@Serializable
data class GroupActivitiesDto(
    val groupId: Int,
    val groupName: String,
    val activities: List<ActivityDto>
)