package com.japp.models.domain

import com.japp.models.Currency
import com.japp.models.ExpenseCategory
import com.japp.models.SplitType

data class Expense(
    val id: Int,
    val groupId: Int,
    val paidBy: Int,
    val amount: Double,
    val currency: Currency,
    val description: String,
    val category: ExpenseCategory?,
    val splitType: SplitType,
    val createdAt: String,
    val updatedAt: String
)