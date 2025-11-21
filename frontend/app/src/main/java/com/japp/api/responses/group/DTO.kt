package com.japp.api.responses.group

data class GroupDto(
    val id: Int,
    val name: String,
    val description: String?,
    val inviteCode: String,
    val createdBy: Int,
    val memberCount: Int,
    val totalExpenses: Double,
    val createdAt: String
)

data class GroupMemberDto(
    val userId: Int,
    val username: String,
    val userEmail: String,
    val joinedAt: String,
    val isOwner: Boolean
)

data class GroupInviteDetailsDto(
    val inviteCode: String,
    val deepLink: String,
    val groupId: Int,
    val groupName: String,
    val memberCount: Int
)

data class GroupPreviewDto(
    val id: Int,
    val name: String,
    val description: String?,
    val memberCount: Int,
    val createdAt: String
)

data class DebtHistoryDto(
    val id: Int,
    val groupId: Int,
    val userId: Int,
    val username: String,
    val amountOwed: Double,
    val leftAt: String
)