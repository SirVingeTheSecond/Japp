package com.example.japp.api.responses.group

data class CreateGroupRequest(
    val name: String,
    val description: String? = null
)

data class JoinGroupRequest(
    val inviteCode: String
)

data class AddMemberRequest(
    val userId: Int
)