package com.example.japp.api.responses.settlement

data class SettlementDto(
    val id: Int,
    val groupId: Int,
    val fromUserId: Int,
    val fromUserName: String,
    val toUserId: Int,
    val toUserName: String,
    val amount: Double,
    val completed: Boolean,
    val createdAt: String,
    val completedAt: String?
)

data class SettlementSuggestionDto(
    val fromUserId: Int,
    val fromUserName: String,
    val toUserId: Int,
    val toUserName: String,
    val amount: Double
)

data class GroupSettlementSuggestionsDto(
    val groupId: Int,
    val groupName: String,
    val suggestions: List<SettlementSuggestionDto>
)