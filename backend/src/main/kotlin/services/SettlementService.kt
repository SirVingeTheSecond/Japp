package com.japp.services

import com.japp.models.*
import com.japp.models.domain.Settlement
import com.japp.models.dto.*
import com.japp.repositories.ISettlementRepository
import com.japp.repositories.IGroupRepository
import com.japp.repositories.IUserRepository
import com.japp.repositories.IExpenseRepository
import com.japp.validation.SettlementValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.math.abs
import kotlin.math.min

class SettlementService(
    private val settlementRepository: ISettlementRepository,
    private val groupRepository: IGroupRepository,
    private val userRepository: IUserRepository,
    private val expenseRepository: IExpenseRepository
) {

    suspend fun getSettlementSuggestions(
        groupId: Int,
        userId: Int
    ): Result<GroupSettlementSuggestionsDto, SettlementError> {
        return withContext(Dispatchers.IO) {
            try {
                transaction {
                    if (!groupRepository.isMember(groupId, userId)) {
                        return@transaction Result.Failure(SettlementError.NotMember(groupId))
                    }

                    val group = groupRepository.findById(groupId) ?: return@transaction Result.Failure(
                            SettlementError.InternalError("Group not found")
                        )

                    val balances = expenseRepository.calculateGroupBalances(groupId)
                    val suggestions = minimizeCashFlow(balances)

                    val suggestionDtos = suggestions.map { (from, to, amount) ->
                        val fromUser = userRepository.findById(from)
                        val toUser = userRepository.findById(to)

                        SettlementSuggestionDto(
                            fromUserId = from,
                            fromUserName = fromUser?.name ?: "Unknown",
                            toUserId = to,
                            toUserName = toUser?.name ?: "Unknown",
                            amount = amount
                        )
                    }

                    Result.Success(
                        GroupSettlementSuggestionsDto(
                            groupId = groupId,
                            groupName = group.name,
                            suggestions = suggestionDtos
                        )
                    )
                }
            } catch (e: Exception) {
                Result.Failure(
                    SettlementError.InternalError(e.message ?: "Failed to calculate settlements")
                )
            }
        }
    }

    suspend fun createSettlement(
        request: CreateSettlementRequest,
        userId: Int
    ): Result<SettlementDto, SettlementError> {
        return when (val validation = SettlementValidator.validateCreateSettlement(request)) {
            is Result.Failure -> validation
            is Result.Success -> {
                withContext(Dispatchers.IO) {
                    try {
                        transaction {
                            if (!groupRepository.isMember(request.groupId, userId)) {
                                return@transaction Result.Failure(
                                    SettlementError.NotMember(request.groupId)
                                )
                            }

                            if (!groupRepository.isMember(request.groupId, request.toUserId)) {
                                return@transaction Result.Failure(
                                    SettlementError.ValidationError("Recipient is not a member of this group")
                                )
                            }

                            if (userId == request.toUserId) {
                                return@transaction Result.Failure(
                                    SettlementError.ValidationError("Cannot create settlement to yourself")
                                )
                            }

                            val settlement = settlementRepository.create(
                                groupId = request.groupId,
                                fromUserId = userId,
                                toUserId = request.toUserId,
                                amount = request.amount
                            )

                            Result.Success(toSettlementDto(settlement))
                        }
                    } catch (e: Exception) {
                        Result.Failure(
                            SettlementError.InternalError(e.message ?: "Failed to create settlement")
                        )
                    }
                }
            }
        }
    }

    suspend fun markSettlementCompleted(
        settlementId: Int,
        userId: Int
    ): Result<SettlementDto, SettlementError> {
        return withContext(Dispatchers.IO) {
            try {
                transaction {
                    val settlement = settlementRepository.findById(settlementId)
                        ?: return@transaction Result.Failure(SettlementError.NotFound(settlementId))

                    if (!groupRepository.isMember(settlement.groupId, userId)) {
                        return@transaction Result.Failure(
                            SettlementError.NotMember(settlement.groupId)
                        )
                    }

                    if (settlement.toUserId != userId) {
                        return@transaction Result.Failure(
                            SettlementError.Unauthorized("Only the recipient can mark settlement as completed")
                        )
                    }

                    if (settlement.completed) {
                        return@transaction Result.Failure(
                            SettlementError.ValidationError("Settlement is already completed")
                        )
                    }

                    val updatedSettlement = settlementRepository.markAsCompleted(settlementId)
                        ?: return@transaction Result.Failure(
                            SettlementError.InternalError("Failed to update settlement")
                        )

                    Result.Success(toSettlementDto(updatedSettlement))
                }
            } catch (e: Exception) {
                Result.Failure(
                    SettlementError.InternalError(e.message ?: "Failed to complete settlement")
                )
            }
        }
    }

    suspend fun getGroupSettlements(
        groupId: Int,
        userId: Int,
        pendingOnly: Boolean = false
    ): Result<List<SettlementDto>, SettlementError> {
        return withContext(Dispatchers.IO) {
            try {
                transaction {
                    if (!groupRepository.isMember(groupId, userId)) {
                        return@transaction Result.Failure(SettlementError.NotMember(groupId))
                    }

                    val settlements = if (pendingOnly) {
                        settlementRepository.findPendingByGroupId(groupId)
                    } else {
                        settlementRepository.findByGroupId(groupId)
                    }

                    val settlementDtos = settlements.map { toSettlementDto(it) }
                    Result.Success(settlementDtos)
                }
            } catch (e: Exception) {
                Result.Failure(
                    SettlementError.InternalError(e.message ?: "Failed to retrieve settlements")
                )
            }
        }
    }

    // This runs with a time complexity of O(n^2)
    private fun minimizeCashFlow(balances: Map<Int, Double>): List<Triple<Int, Int, Double>> {
        val settlements = mutableListOf<Triple<Int, Int, Double>>()
        val netBalances = balances.toMutableMap()

        while (netBalances.values.any { abs(it) > 0.01 }) {
            val maxCreditor = netBalances.maxByOrNull { it.value }?.key ?: break
            val maxDebtor = netBalances.minByOrNull { it.value }?.key ?: break

            val maxCredit = netBalances[maxCreditor] ?: 0.0
            val maxDebt = netBalances[maxDebtor] ?: 0.0

            if (abs(maxCredit) < 0.01 || abs(maxDebt) < 0.01) break

            val settleAmount = min(maxCredit, abs(maxDebt))

            settlements.add(Triple(maxDebtor, maxCreditor, settleAmount))

            netBalances[maxCreditor] = maxCredit - settleAmount
            netBalances[maxDebtor] = maxDebt + settleAmount
        }

        return settlements
    }

    private fun toSettlementDto(settlement: Settlement): SettlementDto {
        val fromUser = userRepository.findById(settlement.fromUserId)
        val toUser = userRepository.findById(settlement.toUserId)

        return SettlementDto(
            id = settlement.id,
            groupId = settlement.groupId,
            fromUserId = settlement.fromUserId,
            fromUserName = fromUser?.name ?: "Unknown",
            toUserId = settlement.toUserId,
            toUserName = toUser?.name ?: "Unknown",
            amount = settlement.amount,
            completed = settlement.completed,
            createdAt = settlement.createdAt,
            completedAt = settlement.completedAt
        )
    }
}