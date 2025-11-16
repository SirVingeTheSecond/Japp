package com.japp.models.dto

import com.japp.models.SettlementStatus
import kotlinx.serialization.Serializable

@Serializable
data class CreateSettlementRequest(
    val groupId: Int,
    val toUserId: Int,
    val amount: Double
)

@Serializable
data class SettlementDto(
    val id: Int,
    val groupId: Int,
    val fromUserId: Int,
    val fromUserName: String,
    val toUserId: Int,
    val toUserName: String,
    val amount: Double,
    val status: SettlementStatus,
    val createdAt: String,
    val completedAt: String?
)

@Serializable
data class SettlementSuggestionDto(
    val fromUserId: Int,
    val fromUserName: String,
    val toUserId: Int,
    val toUserName: String,
    val amount: Double
)

@Serializable
data class GroupSettlementSuggestionsDto(
    val groupId: Int,
    val groupName: String,
    val suggestions: List<SettlementSuggestionDto>
)