package com.japp.services

import com.japp.models.*
import com.japp.models.domain.Expense
import com.japp.models.domain.Group
import com.japp.models.domain.User
import com.japp.models.dto.*
import com.japp.models.error.AppError
import com.japp.services.interfaces.IExpenseRepository
import com.japp.services.interfaces.IGroupRepository
import com.japp.services.interfaces.ISettlementRepository
import com.japp.services.interfaces.IUserRepository
import com.japp.utils.toDto
import com.japp.utils.createBalanceDto
import com.japp.validation.ExpenseValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.jdbc.transactions.transaction


class ExpenseService(
    private val expenseRepository: IExpenseRepository,
    private val groupRepository: IGroupRepository,
    private val userRepository: IUserRepository,
    private val settlementRepository: ISettlementRepository,
    private val activityService: ActivityService,
    private val messageService: MessageService,
    private val notificationService: NotificationService
) {

    private data class ExpenseCreationData(
        val expenseDto: ExpenseDto,
        val user: User?,
        val group: Group?,
        val members: List<Int>
    )

    // HOLY SHIT THIS IS A HUGE FUNCTION
    suspend fun createExpense(
        request: CreateExpenseRequest,
        userId: Int
    ): Result<ExpenseDto, AppError> {
        return when (val validation = ExpenseValidator.validateCreateExpense(request)) {
            is Result.Failure -> validation
            is Result.Success -> {
                withContext(Dispatchers.IO) {
                    try {
                        val result = transaction {
                            if (!groupRepository.isMember(request.groupId, userId)) {
                                return@transaction Result.Failure(
                                    AppError.NotMember(request.groupId)
                                )
                            }

                            val expense = expenseRepository.create(
                                groupId = request.groupId,
                                paidBy = userId,
                                amount = request.amount,
                                currency = request.currency,
                                description = request.description,
                                category = request.category,
                                splitType = request.splitType
                            )

                            val members = groupRepository.getMembers(request.groupId)

                            when (request.splitType) {
                                SplitType.EQUAL -> {
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
                                SplitType.CUSTOM -> {
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

                            val user = userRepository.findById(userId)
                            val group = groupRepository.findById(request.groupId)
                            val expenseDto = toExpenseDto(expense, userId)

                            Result.Success(
                                ExpenseCreationData(
                                    expenseDto = expenseDto,
                                    user = user,
                                    group = group,
                                    members = members
                                )
                            )
                        }

                        when (result) {
                            is Result.Success -> {
                                val data = result.value

                                // Log activity
                                activityService.logExpenseCreated(
                                    groupId = request.groupId,
                                    userId = userId,
                                    expenseId = data.expenseDto.id,
                                    amount = request.amount,
                                    currency = request.currency.code,
                                    description = request.description
                                )

                                // System message
                                messageService.createSystemMessage(
                                    groupId = request.groupId,
                                    content = "${data.user?.username ?: "Someone"} added expense: ${request.description} - ${request.amount} ${request.currency.code}"
                                )

                                // Send notifications
                                launch(Dispatchers.IO) {
                                    if (data.group != null) {
                                        notificationService.notifyExpenseCreated(
                                            groupId = request.groupId,
                                            groupName = data.group.name,
                                            expenseDescription = request.description,
                                            amount = request.amount,
                                            createdByUsername = data.user?.username ?: "Someone",
                                            memberUserIds = data.members,
                                            excludeUserId = userId
                                        )
                                    }
                                }

                                Result.Success(data.expenseDto)
                            }
                            is Result.Failure -> result
                        }
                    } catch (e: Exception) {
                        Result.Failure(
                            AppError.Internal(e.message ?: "Failed to create expense")
                        )
                    }
                }
            }
        }
    }

    suspend fun getGroupExpenses(
        groupId: Int,
        userId: Int
    ): Result<List<ExpenseDto>, AppError> {
        return withContext(Dispatchers.IO) {
            try {
                transaction {
                    if (!groupRepository.isMember(groupId, userId)) {
                        return@transaction Result.Failure(AppError.NotMember(groupId))
                    }

                    val expenses = expenseRepository.findByGroupId(groupId)
                    val expenseDtos = expenses.map { expense ->
                        toExpenseDto(expense, expense.paidBy)
                    }

                    Result.Success(expenseDtos)
                }
            } catch (e: Exception) {
                Result.Failure(
                    AppError.Internal(e.message ?: "Failed to retrieve expenses")
                )
            }
        }
    }

    suspend fun getGroupBalances(
        groupId: Int,
        userId: Int
    ): Result<GroupBalanceSummaryDto, AppError> {
        return withContext(Dispatchers.IO) {
            try {
                transaction {
                    if (!groupRepository.isMember(groupId, userId)) {
                        return@transaction Result.Failure(AppError.NotMember(groupId))
                    }

                    val group = groupRepository.findById(groupId)
                        ?: return@transaction Result.Failure(AppError.Internal("Group not found"))

                    val balances = calculateNetBalances(groupId)

                    val balanceDtos = balances.map { (userId, balance) ->
                        val user = userRepository.findById(userId)
                        createBalanceDto(
                            userId = userId,
                            username = user?.username ?: "Unknown",
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
                Result.Failure(AppError.Internal(e.message ?: "Failed to calculate balances"))
            }
        }
    }

    suspend fun deleteExpense(
        expenseId: Int,
        userId: Int
    ): Result<Unit, AppError> {
        return withContext(Dispatchers.IO) {
            try {
                var username: String? = null

                val result = transaction {
                    val expense = expenseRepository.findById(expenseId)
                        ?: return@transaction Result.Failure(AppError.NotFound("Expense", expenseId))

                    if (expense.paidBy != userId) {
                        return@transaction Result.Failure(
                            AppError.Unauthorized("Only the payer can delete this expense")
                        )
                    }

                    val user = userRepository.findById(userId)
                    username = user?.username

                    expenseRepository.delete(expenseId)
                    groupRepository.updateTotalExpenses(expense.groupId, -expense.amount)

                    Result.Success(Triple(expense.groupId, expense.description, expense.amount))
                }

                when (result) {
                    is Result.Success -> {
                        val (groupId, description, amount) = result.value

                        activityService.logExpenseDeleted(
                            groupId = groupId,
                            userId = userId,
                            expenseId = expenseId,
                            amount = amount,
                            description = description
                        )

                        messageService.createSystemMessage(
                            groupId = groupId,
                            content = "${username ?: "Someone"} deleted expense: $description"
                        )

                        Result.Success(Unit)
                    }
                    is Result.Failure -> result
                }
            } catch (e: Exception) {
                Result.Failure(
                    AppError.Internal(e.message ?: "Failed to delete expense")
                )
            }
        }
    }

    /**
     * Calculate net balances for a group including completed settlements.
     * Must be called within a transaction.
     */
    fun calculateNetBalances(groupId: Int): Map<Int, Double> {
        val balances = expenseRepository.calculateGroupBalances(groupId).toMutableMap()

        val completedSettlements = settlementRepository.findByGroupId(groupId)
            .filter { it.status == SettlementStatus.COMPLETED }

        completedSettlements.forEach { settlement ->
            balances[settlement.fromUserId] =
                balances.getOrDefault(settlement.fromUserId, 0.0) + settlement.amount
            balances[settlement.toUserId] =
                balances.getOrDefault(settlement.toUserId, 0.0) - settlement.amount
        }

        return balances
    }

    private fun toExpenseDto(expense: Expense, paidById: Int): ExpenseDto {
        val payer = userRepository.findById(paidById)
        val splits = expenseRepository.getSplits(expense.id)

        val splitDtos = splits.map { split ->
            val user = userRepository.findById(split.userId)
            split.toDto(username = user?.username ?: "Unknown")
        }

        return expense.toDto(
            paidByName = payer?.username ?: "Unknown",
            splits = splitDtos
        )
    }
}