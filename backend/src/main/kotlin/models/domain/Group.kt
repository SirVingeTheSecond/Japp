package com.japp.models.domain

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