package com.japp.models.domain

data class Expense(
    val id: Int,
    val groupId: Int,
    val paidBy: Int,
    val amount: Double,
    val currency: String,
    val description: String,
    val category: String?,
    val splitType: String,
    val createdAt: String,
    val updatedAt: String
)