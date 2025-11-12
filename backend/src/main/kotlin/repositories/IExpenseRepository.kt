package com.japp.repositories

import com.japp.models.domain.Expense
import com.japp.models.domain.ExpenseSplit

interface IExpenseRepository {
    fun create(
        groupId: Int,
        paidBy: Int,
        amount: Double,
        currency: String,
        description: String,
        category: String?,
        splitType: String
    ): Expense

    fun createSplit(
        expenseId: Int,
        userId: Int,
        shareAmount: Double?,
        sharePercentage: Double?
    ): ExpenseSplit

    fun findById(expenseId: Int): Expense?
    fun findByGroupId(groupId: Int): List<Expense>
    fun getSplits(expenseId: Int): List<ExpenseSplit>
    fun delete(expenseId: Int): Boolean
    fun calculateGroupBalances(groupId: Int): Map<Int, Double>
}