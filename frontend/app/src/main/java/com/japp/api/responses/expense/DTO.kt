package com.japp.api.responses.expense

import com.japp.api.responses.Currency
import com.japp.api.responses.ExpenseCategory
import com.japp.api.responses.SplitType

data class ExpenseDto(
    val id: Int,
    val groupId: Int,
    val paidBy: Int,
    val paidByName: String,
    val amount: Double,
    val currency: Currency,
    val description: String,
    val category: ExpenseCategory?,
    val splitType: SplitType,
    val splits: List<ExpenseSplitDto>,
    val createdAt: String
)

data class ExpenseSplitDto(
    val userId: Int,
    val username: String,
    val shareAmount: Double?,
    val sharePercentage: Double?
)

data class BalanceDto(
    val userId: Int,
    val username: String,
    val balance: Double
)

data class GroupBalanceSummaryDto(
    val groupId: Int,
    val groupName: String,
    val balances: List<BalanceDto>
)

data class CurrencyDto(
    val code: String
)