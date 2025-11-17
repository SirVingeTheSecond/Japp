package com.japp.models.dto

import com.japp.models.Currency
import com.japp.models.ExpenseCategory
import com.japp.models.SplitType
import kotlinx.serialization.Serializable

@Serializable
data class CreateExpenseRequest(
    val groupId: Int,
    val amount: Double,
    val description: String,
    val category: ExpenseCategory? = null,
    val currency: Currency = Currency.DKK,
    val splitType: SplitType = SplitType.EQUAL,
    val splits: List<ExpenseSplitRequest>? = null
)

@Serializable
data class ExpenseSplitRequest(
    val userId: Int,
    val shareAmount: Double? = null,
    val sharePercentage: Double? = null
)

@Serializable
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

@Serializable
data class ExpenseSplitDto(
    val userId: Int,
    val username: String,
    val shareAmount: Double?,
    val sharePercentage: Double?
)

@Serializable
data class BalanceDto(
    val userId: Int,
    val username: String,
    val balance: Double
)

@Serializable
data class GroupBalanceSummaryDto(
    val groupId: Int,
    val groupName: String,
    val balances: List<BalanceDto>
)