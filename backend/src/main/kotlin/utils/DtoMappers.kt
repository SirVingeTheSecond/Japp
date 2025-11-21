package com.japp.utils

import com.japp.models.domain.*
import com.japp.models.dto.*

fun createGroupMemberDto(
    user: User,
    joinedAt: String,
    isOwner: Boolean
) = GroupMemberDto(
    userId = user.id,
    username = user.username,
    userEmail = user.email,
    joinedAt = joinedAt,
    isOwner = isOwner
)

fun createDebtHistoryDto(
    debtHistory: DebtHistory,
    username: String
) = DebtHistoryDto(
    id = debtHistory.id,
    groupId = debtHistory.groupId,
    userId = debtHistory.userId,
    username = username,
    amountOwed = debtHistory.amountOwed,
    leftAt = debtHistory.leftAt
)

fun User.toDto() = UserDto(
    id = id,
    email = email,
    username = username,
    firstname = firstname,
    lastname = lastname,
    phone = phone,
    profilePicture = profilePicture
)

fun Group.toDto() = GroupDto(
    id = id,
    name = name,
    description = description,
    inviteCode = inviteCode,
    createdBy = createdBy,
    memberCount = memberCount,
    totalExpenses = totalExpenses,
    createdAt = createdAt
)

fun Expense.toDto(
    paidByName: String,
    splits: List<ExpenseSplitDto>
) = ExpenseDto(
    id = id,
    groupId = groupId,
    paidBy = paidBy,
    paidByName = paidByName,
    amount = amount,
    currency = currency,
    description = description,
    category = category,
    splitType = splitType,
    splits = splits,
    createdAt = createdAt
)

fun ExpenseSplit.toDto(username: String) = ExpenseSplitDto(
    userId = userId,
    username = username,
    shareAmount = shareAmount,
    sharePercentage = sharePercentage
)

fun Settlement.toDto(
    fromUserName: String,
    toUserName: String
) = SettlementDto(
    id = id,
    groupId = groupId,
    fromUserId = fromUserId,
    fromUserName = fromUserName,
    toUserId = toUserId,
    toUserName = toUserName,
    amount = amount,
    status = status,
    createdAt = createdAt,
    completedAt = completedAt
)

fun ActivityLog.toDto(
    userName: String,
    metadata: Map<String, String>
) = ActivityDto(
    id = id,
    groupId = groupId,
    userId = userId,
    userName = userName,
    actionType = actionType,
    description = description,
    relatedExpenseId = relatedExpenseId,
    relatedSettlementId = relatedSettlementId,
    metadata = metadata,
    createdAt = createdAt
)

fun createBalanceDto(
    userId: Int,
    username: String,
    balance: Double
) = BalanceDto(
    userId = userId,
    username = username,
    balance = balance
)



fun Message.toDto(
    userName: String?,
    readByUserIds: List<Int> = emptyList()
) = MessageDto(
    id = id,
    groupId = groupId,
    userId = userId,
    userName = userName,
    content = content,
    messageType = messageType,
    createdAt = createdAt,
    editedAt = editedAt,
    isDeleted = deletedAt != null,
    readBy = readByUserIds
)

fun Attachment.toDto(
    uploaderName: String,
    downloadUrl: String
) = AttachmentDto(
    id = id,
    expenseId = expenseId,
    uploadedBy = uploadedBy,
    uploaderName = uploaderName,
    fileName = fileName,
    fileSize = fileSize,
    mimeType = mimeType,
    uploadedAt = uploadedAt,
    downloadUrl = downloadUrl
)