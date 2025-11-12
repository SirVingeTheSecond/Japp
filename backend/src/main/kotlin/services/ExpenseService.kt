package com.japp.services

import com.japp.models.*
import com.japp.models.domain.Expense
import com.japp.models.dto.*
import com.japp.repositories.IExpenseRepository
import com.japp.repositories.IGroupRepository
import com.japp.repositories.IUserRepository
import com.japp.validation.ExpenseValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class ExpenseService(
    private val expenseRepository: IExpenseRepository,
    private val groupRepository: IGroupRepository,
    private val userRepository: IUserRepository,
    private val activityService: ActivityService
) {

    suspend fun createExpense(
        request: CreateExpenseRequest,
        userId: Int
    ): Result<ExpenseDto, ExpenseError> {
        return when (val validation = ExpenseValidator.validateCreateExpense(request)) {
            is Result.Failure -> validation
            is Result.Success -> {
                withContext(Dispatchers.IO) {
                    try {
                        transaction {
                            if (!groupRepository.isMember(request.groupId, userId)) {
                                return@transaction Result.Failure(
                                    ExpenseError.NotMember(request.groupId)
                                )
                            }

                            val expense = expenseRepository.create(
                                groupId = request.groupId,
                                paidBy = userId,
                                amount = request.amount,
                                currency = "DKK",
                                description = request.description,
                                category = request.category,
                                splitType = request.splitType
                            )

                            val members = groupRepository.getMembers(request.groupId)

                            when (request.splitType) {
                                "equal" -> {
                                    val shareAmount = request.amount / members.size
                                    members.forEach { memberId ->
                                        expenseRepository.createSplit(
                                            expenseId = expense.id,
                                            userId = memberId,
                                            shareAmount = shareAmount,
                                            sharePercentage = null
                                        )
                                    }
                                }
                                "custom" -> {
                                    request.splits?.forEach { split ->
                                        val shareAmount = split.shareAmount
                                            ?: (split.sharePercentage?.let { it / 100.0 * request.amount })

                                        expenseRepository.createSplit(
                                            expenseId = expense.id,
                                            userId = split.userId,
                                            shareAmount = shareAmount,
                                            sharePercentage = split.sharePercentage
                                        )
                                    }
                                }
                            }

                            groupRepository.updateTotalExpenses(request.groupId, request.amount)


                            activityService.logExpenseCreated(
                                groupId = request.groupId,
                                userId = userId,
                                expenseId = expense.id,
                                amount = request.amount,
                                currency = "DKK",
                                description = request.description
                            )

                            Result.Success(toExpenseDto(expense, userId))
                        }
                    } catch (e: Exception) {
                        Result.Failure(
                            ExpenseError.InternalError(e.message ?: "Failed to create expense")
                        )
                    }
                }
            }
        }
    }

    suspend fun getGroupExpenses(
        groupId: Int,
        userId: Int
    ): Result<List<ExpenseDto>, ExpenseError> {
        return withContext(Dispatchers.IO) {
            try {
                transaction {
                    if (!groupRepository.isMember(groupId, userId)) {
                        return@transaction Result.Failure(ExpenseError.NotMember(groupId))
                    }

                    val expenses = expenseRepository.findByGroupId(groupId)
                    val expenseDtos = expenses.map { expense ->
                        toExpenseDto(expense, expense.paidBy)
                    }

                    Result.Success(expenseDtos)
                }
            } catch (e: Exception) {
                Result.Failure(
                    ExpenseError.InternalError(e.message ?: "Failed to retrieve expenses")
                )
            }
        }
    }

    suspend fun getGroupBalances(
        groupId: Int,
        userId: Int
    ): Result<GroupBalanceSummaryDto, ExpenseError> {
        return withContext(Dispatchers.IO) {
            try {
                transaction {
                    if (!groupRepository.isMember(groupId, userId)) {
                        return@transaction Result.Failure(ExpenseError.NotMember(groupId))
                    }

                    val group = groupRepository.findById(groupId)
                        ?: return@transaction Result.Failure(
                            ExpenseError.InternalError("Group not found")
                        )

                    val balances = expenseRepository.calculateGroupBalances(groupId)

                    val balanceDtos = balances.map { (userId, balance) ->
                        val user = userRepository.findById(userId)
                        BalanceDto(
                            userId = userId,
                            userName = user?.name ?: "Unknown",
                            balance = balance
                        )
                    }.sortedByDescending { it.balance }

                    Result.Success(
                        GroupBalanceSummaryDto(
                            groupId = groupId,
                            groupName = group.name,
                            balances = balanceDtos
                        )
                    )
                }
            } catch (e: Exception) {
                Result.Failure(
                    ExpenseError.InternalError(e.message ?: "Failed to calculate balances")
                )
            }
        }
    }

    suspend fun deleteExpense(
        expenseId: Int,
        userId: Int
    ): Result<Unit, ExpenseError> {
        return withContext(Dispatchers.IO) {
            try {
                transaction {
                    val expense = expenseRepository.findById(expenseId)
                        ?: return@transaction Result.Failure(ExpenseError.NotFound(expenseId))

                    if (expense.paidBy != userId) {
                        return@transaction Result.Failure(
                            ExpenseError.Unauthorized("Only the payer can delete this expense")
                        )
                    }

                    expenseRepository.delete(expenseId)
                    groupRepository.updateTotalExpenses(expense.groupId, -expense.amount)

                    activityService.logExpenseDeleted(
                        groupId = expense.groupId,
                        userId = userId,
                        expenseId = expenseId,
                        amount = expense.amount,
                        description = expense.description
                    )

                    Result.Success(Unit)
                }
            } catch (e: Exception) {
                Result.Failure(
                    ExpenseError.InternalError(e.message ?: "Failed to delete expense")
                )
            }
        }
    }

    private fun toExpenseDto(expense: Expense, paidById: Int): ExpenseDto {
        val payer = userRepository.findById(paidById)
        val splits = expenseRepository.getSplits(expense.id)

        val splitDtos = splits.map { split ->
            val user = userRepository.findById(split.userId)
            ExpenseSplitDto(
                userId = split.userId,
                userName = user?.name ?: "Unknown",
                shareAmount = split.shareAmount,
                sharePercentage = split.sharePercentage
            )
        }

        return ExpenseDto(
            id = expense.id,
            groupId = expense.groupId,
            paidBy = expense.paidBy,
            paidByName = payer?.name ?: "Unknown",
            amount = expense.amount,
            currency = expense.currency,
            description = expense.description,
            category = expense.category,
            splitType = expense.splitType,
            splits = splitDtos,
            createdAt = expense.createdAt
        )
    }
}