package com.japp.repositories.implementation

import com.japp.database.tables.Expenses
import com.japp.database.tables.ExpenseSplits
import com.japp.models.*
import com.japp.models.domain.Expense
import com.japp.models.domain.ExpenseSplit
import com.japp.repositories.interfaces.IExpenseRepository
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class ExpenseRepository : IExpenseRepository {

    init {
        transaction {
            SchemaUtils.create(Expenses, ExpenseSplits)
        }
    }

    override fun create(
        groupId: Int,
        paidBy: Int,
        amount: Double,
        currency: Currency,
        description: String,
        category: ExpenseCategory?,
        splitType: SplitType
    ): Expense {
        val timestamp = System.currentTimeMillis().toString()

        val expenseId = Expenses.insert {
            it[Expenses.groupId] = groupId
            it[Expenses.paidBy] = paidBy
            it[Expenses.amount] = amount
            it[Expenses.currency] = currency.code
            it[Expenses.description] = description
            it[Expenses.category] = category?.value
            it[Expenses.splitType] = splitType.value
            it[createdAt] = timestamp
            it[updatedAt] = timestamp
        }[Expenses.id]

        return findById(expenseId)!!
    }

    override fun createSplit(
        expenseId: Int,
        userId: Int,
        shareAmount: Double?,
        sharePercentage: Double?
    ): ExpenseSplit {
        val splitId = ExpenseSplits.insert {
            it[ExpenseSplits.expenseId] = expenseId
            it[ExpenseSplits.userId] = userId
            it[ExpenseSplits.shareAmount] = shareAmount
            it[ExpenseSplits.sharePercentage] = sharePercentage
        }[ExpenseSplits.id]

        return ExpenseSplit(
            id = splitId,
            expenseId = expenseId,
            userId = userId,
            shareAmount = shareAmount,
            sharePercentage = sharePercentage
        )
    }

    override fun findById(expenseId: Int): Expense? {
        return Expenses.selectAll()
            .where { Expenses.id eq expenseId }
            .map { rowToExpense(it) }
            .singleOrNull()
    }

    override fun findByGroupId(groupId: Int): List<Expense> {
        return Expenses.selectAll()
            .where { Expenses.groupId eq groupId }
            .orderBy(Expenses.createdAt to SortOrder.DESC)
            .map { rowToExpense(it) }
    }

    override fun getSplits(expenseId: Int): List<ExpenseSplit> {
        return ExpenseSplits.selectAll()
            .where { ExpenseSplits.expenseId eq expenseId }
            .map { rowToExpenseSplit(it) }
    }

    override fun delete(expenseId: Int): Boolean {
        ExpenseSplits.deleteWhere { ExpenseSplits.expenseId eq expenseId }
        val deleted = Expenses.deleteWhere { Expenses.id eq expenseId }
        return deleted > 0
    }

    override fun calculateGroupBalances(groupId: Int): Map<Int, Double> {
        val expenses = findByGroupId(groupId)
        val balances = mutableMapOf<Int, Double>()

        expenses.forEach { expense ->
            val splits = getSplits(expense.id)

            balances[expense.paidBy] = balances.getOrDefault(expense.paidBy, 0.0) + expense.amount

            splits.forEach { split ->
                val shareAmount = split.shareAmount ?: 0.0
                balances[split.userId] = balances.getOrDefault(split.userId, 0.0) - shareAmount
            }
        }

        return balances
    }

    private fun rowToExpense(row: ResultRow) = Expense(
        id = row[Expenses.id],
        groupId = row[Expenses.groupId],
        paidBy = row[Expenses.paidBy],
        amount = row[Expenses.amount],
        currency = Currency.fromCode(row[Expenses.currency]) ?: Currency.DKK,
        description = row[Expenses.description],
        category = row[Expenses.category]?.let { ExpenseCategory.fromString(it) },
        splitType = SplitType.fromString(row[Expenses.splitType]) ?: SplitType.EQUAL,
        createdAt = row[Expenses.createdAt],
        updatedAt = row[Expenses.updatedAt]
    )

    private fun rowToExpenseSplit(row: ResultRow) = ExpenseSplit(
        id = row[ExpenseSplits.id],
        expenseId = row[ExpenseSplits.expenseId],
        userId = row[ExpenseSplits.userId],
        shareAmount = row[ExpenseSplits.shareAmount],
        sharePercentage = row[ExpenseSplits.sharePercentage]
    )
}