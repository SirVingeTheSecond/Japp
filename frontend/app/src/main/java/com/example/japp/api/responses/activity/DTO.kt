package com.example.japp.api.responses.activity

import com.example.japp.api.responses.ActivityType

data class ActivityDto(
    val id: Int,
    val groupId: Int,
    val userId: Int,
    val userName: String,
    val actionType: ActivityType,
    val description: String,
    val relatedExpenseId: Int?,
    val relatedSettlementId: Int?,
    val metadata: Map<String, String>,
    val createdAt: String
)

data class GroupActivitiesDto(
    val groupId: Int,
    val groupName: String,
    val activities: List<ActivityDto>
)