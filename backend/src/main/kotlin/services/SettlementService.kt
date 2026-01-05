package com.japp.services

import com.japp.models.*
import com.japp.models.domain.Group
import com.japp.models.dto.*
import com.japp.models.error.AppError
import com.japp.services.interfaces.ISettlementRepository
import com.japp.services.interfaces.IGroupRepository
import com.japp.services.interfaces.IUserRepository
import com.japp.services.interfaces.IExpenseRepository
import com.japp.utils.toDto
import com.japp.validation.SettlementValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.math.abs
import kotlin.math.min

class SettlementService(
    private val settlementRepository: ISettlementRepository,
    private val groupRepository: IGroupRepository,
    private val userRepository: IUserRepository,
    private val expenseService: ExpenseService,
    private val activityService: ActivityService,
    private val messageService: MessageService,
    private val notificationService: NotificationService
) {

    private data class SettlementCompletionData(
        val settlementDto: SettlementDto,
        val currentUsername: String?,
        val fromUsername: String?,
        val group: Group?,
        val members: List<Int>,
        val groupId: Int,
        val amount: Double
    )

    suspend fun createSettlement(
        request: CreateSettlementRequest,
        userId: Int
    ): Result<SettlementDto, AppError> {
        return when (val validation = SettlementValidator.validateCreateSettlement(request)) {
            is Result.Failure -> validation
            is Result.Success -> {
                withContext(Dispatchers.IO) {
                    try {
                        var fromUsername: String? = null
                        var toUsername: String? = null

                        val settlementResult = transaction {
                            if (!groupRepository.isMember(request.groupId, userId)) {
                                return@transaction Result.Failure(
                                    AppError.NotMember(request.groupId)
                                )
                            }

                            if (!groupRepository.isMember(request.groupId, request.toUserId)) {
                                return@transaction Result.Failure(
                                    AppError.Validation("Recipient is not a member of this group")
                                )
                            }

                            if (userId == request.toUserId) {
                                return@transaction Result.Failure(
                                    AppError.Validation("Cannot create settlement to yourself")
                                )
                            }

                            val settlement = settlementRepository.create(
                                groupId = request.groupId,
                                fromUserId = userId,
                                toUserId = request.toUserId,
                                amount = request.amount
                            )

                            val fromUser = userRepository.findById(userId)
                            val toUser = userRepository.findById(request.toUserId)
                            fromUsername = fromUser?.username
                            toUsername = toUser?.username

                            Result.Success(settlement.toDto(
                                fromUserName = fromUser?.username ?: "Unknown",
                                toUserName = toUser?.username ?: "Unknown"
                            ))
                        }

                        if (settlementResult is Result.Success) {
                            activityService.logSettlementCreated(
                                groupId = request.groupId,
                                userId = userId,
                                settlementId = settlementResult.value.id,
                                toUserId = request.toUserId,
                                amount = request.amount
                            )

                            messageService.createSystemMessage(
                                groupId = request.groupId,
                                content = "${fromUsername ?: "Someone"} recorded payment of ${request.amount} DKK to ${toUsername ?: "Someone"}"
                            )
                        }

                        settlementResult
                    } catch (e: Exception) {
                        Result.Failure(
                            AppError.Internal(e.message ?: "Failed to create settlement")
                        )
                    }
                }
            }
        }
    }

    suspend fun getSettlementSuggestions(
        groupId: Int,
        userId: Int
    ): Result<GroupSettlementSuggestionsDto, AppError> {
        return withContext(Dispatchers.IO) {
            try {
                transaction {
                    if (!groupRepository.isMember(groupId, userId)) {
                        return@transaction Result.Failure(AppError.NotMember(groupId))
                    }

                    val group = groupRepository.findById(groupId) ?: return@transaction Result.Failure(
                        AppError.Internal("Group not found")
                    )

                    val balances = expenseService.calculateNetBalances(groupId)
                    val suggestions = minimizeCashFlow(balances)

                    val suggestionDtos = suggestions.map { (from, to, amount) ->
                        val fromUser = userRepository.findById(from)
                        val toUser = userRepository.findById(to)

                        SettlementSuggestionDto(
                            fromUserId = from,
                            fromUserName = fromUser?.username ?: "Unknown",
                            toUserId = to,
                            toUserName = toUser?.username ?: "Unknown",
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
                    AppError.Internal(e.message ?: "Failed to calculate settlements")
                )
            }
        }
    }

    suspend fun markSettlementCompleted(
        settlementId: Int,
        userId: Int
    ): Result<SettlementDto, AppError> {
        return withContext(Dispatchers.IO) {
            try {
                val completionResult = transaction {
                    val settlement = settlementRepository.findById(settlementId)
                        ?: return@transaction Result.Failure(AppError.NotFound("Settlement", settlementId))

                    if (!groupRepository.isMember(settlement.groupId, userId)) {
                        return@transaction Result.Failure(
                            AppError.NotMember(settlement.groupId)
                        )
                    }

                    if (settlement.toUserId != userId) {
                        return@transaction Result.Failure(
                            AppError.Unauthorized("Only the recipient can mark settlement as completed")
                        )
                    }

                    if (settlement.status == SettlementStatus.COMPLETED) {
                        return@transaction Result.Failure(
                            AppError.Validation("Settlement is already completed")
                        )
                    }

                    val updatedSettlement = settlementRepository.markAsCompleted(settlementId)
                        ?: return@transaction Result.Failure(
                            AppError.Internal("Failed to update settlement")
                        )

                    val currentUser = userRepository.findById(userId)
                    val fromUser = userRepository.findById(settlement.fromUserId)
                    val toUser = userRepository.findById(updatedSettlement.toUserId)
                    val group = groupRepository.findById(settlement.groupId)
                    val members = groupRepository.getMembers(settlement.groupId)

                    Result.Success(
                        SettlementCompletionData(
                            settlementDto = updatedSettlement.toDto(
                                fromUserName = fromUser?.username ?: "Unknown",
                                toUserName = toUser?.username ?: "Unknown"
                            ),
                            currentUsername = currentUser?.username,
                            fromUsername = fromUser?.username,
                            group = group,
                            members = members,
                            groupId = settlement.groupId,
                            amount = settlement.amount
                        )
                    )
                }

                when (completionResult) {
                    is Result.Success -> {
                        val data = completionResult.value

                        activityService.logSettlementCompleted(
                            groupId = data.groupId,
                            userId = userId,
                            settlementId = settlementId,
                            fromUserId = data.settlementDto.fromUserId,
                            amount = data.amount
                        )

                        messageService.createSystemMessage(
                            groupId = data.groupId,
                            content = "${data.currentUsername ?: "Someone"} confirmed payment from ${data.fromUsername ?: "Someone"} - ${data.amount} DKK"
                        )

                        launch(Dispatchers.IO) {
                            if (data.group != null) {
                                notificationService.notifySettlementCompleted(
                                    groupId = data.groupId,
                                    groupName = data.group.name,
                                    fromUsername = data.fromUsername ?: "Someone",
                                    toUsername = data.currentUsername ?: "Someone",
                                    amount = data.amount,
                                    notifyUserIds = data.members
                                )
                            }
                        }

                        Result.Success(data.settlementDto)
                    }
                    is Result.Failure -> completionResult
                }
            } catch (e: Exception) {
                Result.Failure(
                    AppError.Internal(e.message ?: "Failed to complete settlement")
                )
            }
        }
    }

    suspend fun getGroupSettlements(
        groupId: Int,
        userId: Int,
        pendingOnly: Boolean = false
    ): Result<List<SettlementDto>, AppError> {
        return withContext(Dispatchers.IO) {
            try {
                transaction {
                    if (!groupRepository.isMember(groupId, userId)) {
                        return@transaction Result.Failure(AppError.NotMember(groupId))
                    }

                    val settlements = if (pendingOnly) {
                        settlementRepository.findPendingByGroupId(groupId)
                    } else {
                        settlementRepository.findByGroupId(groupId)
                    }

                    val settlementDtos = settlements.map { settlement ->
                        val fromUser = userRepository.findById(settlement.fromUserId)
                        val toUser = userRepository.findById(settlement.toUserId)
                        settlement.toDto(
                            fromUserName = fromUser?.username ?: "Unknown",
                            toUserName = toUser?.username ?: "Unknown"
                        )
                    }

                    Result.Success(settlementDtos)
                }
            } catch (e: Exception) {
                Result.Failure(
                    AppError.Internal(e.message ?: "Failed to retrieve settlements")
                )
            }
        }
    }

    private fun minimizeCashFlow(balances: Map<Int, Double>): List<Triple<Int, Int, Double>> {
        val settlements = mutableListOf<Triple<Int, Int, Double>>()
        val netBalances = balances.toMutableMap()

        while (netBalances.values.any { abs(it) > 0.01 }) {
            val maxCreditor = netBalances.maxByOrNull { it.value }?.key ?: break
            val maxDebtor = netBalances.minByOrNull { it.value }?.key ?: break

            val creditorBalance = netBalances[maxCreditor] ?: break
            val debtorBalance = netBalances[maxDebtor] ?: break

            val settleAmount = min(creditorBalance, abs(debtorBalance))

            settlements.add(Triple(maxDebtor, maxCreditor, settleAmount))

            netBalances[maxCreditor] = creditorBalance - settleAmount
            netBalances[maxDebtor] = debtorBalance + settleAmount
        }

        return settlements
    }
}
