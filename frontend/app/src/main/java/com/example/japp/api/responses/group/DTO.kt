package com.example.japp.api.responses.group

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
    val userName: String,
    val userEmail: String,
    val joinedAt: String,
    val isOwner: Boolean
)