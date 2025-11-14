package com.example.japp.api.responses.group

data class GroupCreateRequest (
    val name: String,
    val description: String? = null
)

data class JoinGroupRequest(
    val inviteCode: String
)