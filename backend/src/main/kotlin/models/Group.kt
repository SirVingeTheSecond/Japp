package com.japp.models

import kotlinx.serialization.Serializable

data class Group(
    val id: Int,
    val name: String,
    val description: String?,
    val inviteCode: String,
    val createdBy: Int,
    val memberCount: Int,
    val totalExpenses: Double,
    val createdAt: String,
    val updatedAt: String
)

/**
 * DTOs for API requests/responses
 */

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
    val createdAt: String,
    val members: List<GroupMemberDto>? = null // This will be fetched separately
)

@Serializable
data class GroupMemberDto(
    val userId: Int,
    val userName: String,
    val userEmail: String,
    val joinedAt: String,
    val isOwner: Boolean // userId == group.createdBy
)

@Serializable
data class GroupListDto(
    val id: Int,
    val name: String,
    val memberCount: Int,
    val totalExpenses: Double,
    val isOwner: Boolean // Based on current user
)