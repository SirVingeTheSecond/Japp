package com.example.japp.api.responses.expense

data class ExpenseDto(
    val id: Int,
    val groupId: Int,
    val paidBy: Int,
    val paidByName: String,
    val amount: Double,
    val currency: String,
    val description: String,
    val category: String?,
    val splitType: String,
    val splits: List<ExpenseSplitDto>,
    val createdAt: String
)

data class ExpenseSplitDto(
    val userId: Int,
    val userName: String,
    val shareAmount: Double?,
    val sharePercentage: Double?
)

data class BalanceDto(
    val userId: Int,
    val userName: String,
    val balance: Double
)

data class GroupBalanceSummaryDto(
    val groupId: Int,
    val groupName: String,
    val balances: List<BalanceDto>
)