package com.japp.models.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateGroupRequest(
    val name: String,
    val description: String? = null
)

@Serializable
data class JoinGroupRequest(
    val inviteCode: String
)

@Serializable
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

@Serializable
data class GroupMemberDto(
    val userId: Int,
    val username: String,
    val userEmail: String,
    val joinedAt: String,
    val isOwner: Boolean
)