package com.japp.services

import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import com.japp.services.interfaces.IGroupRepository
import com.japp.services.interfaces.IUserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.slf4j.LoggerFactory

/**
 * Service for sending push notifications via Firebase Cloud Messaging.
 */
class NotificationService(
    private val firebaseApp: FirebaseApp,
    private val userRepository: IUserRepository,
    private val groupRepository: IGroupRepository
) {
    private val logger = LoggerFactory.getLogger(NotificationService::class.java)

    /**
     * Filter a list of user id for a specific group, to exclude users which have turned off notifications
     */
    private fun filterNotificationPreference(groupId: Int, userIds: List<Int>): List<Int> {
        val result = userIds.toMutableList()

        result.forEach { userId ->
            if (!groupRepository.hasNotificationEnabled(groupId, userId)) { // I know this is not the most efficient way, it should be changed. #TODO
                result.removeAt(result.indexOf(userId))
            }
        }

        return result.toList()
    }

    /**
     * Send notification to specific FCM tokens
     */
    private fun sendToTokens(
        tokens: List<String>,
        title: String,
        body: String,
        data: Map<String, String>
    ): Boolean {
        if (tokens.isEmpty()) return false

        val messaging = FirebaseMessaging.getInstance(firebaseApp)
        var successCount = 0
        var failureCount = 0

        tokens.forEach { token ->
            try {
                val notification = Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build()

                val message = Message.builder()
                    .setToken(token)
                    .setNotification(notification)
                    .putAllData(data)
                    .build()

                val response = messaging.send(message)
                logger.debug("Successfully sent notification to token: ${token.take(20)}... Response: $response")
                successCount++
            } catch (e: Exception) {
                logger.error("Failed to send notification to token: ${token.take(20)}...", e)
                failureCount++

                if (e.message?.contains("registration-token-not-registered") == true) {
                    logger.warn("Invalid FCM token detected: ${token.take(20)}...")
                }
            }
        }

        logger.info("Notification sent: $successCount succeeded, $failureCount failed")
        return successCount > 0
    }

    /**
     * Notify all group members about a new expense
     */
    suspend fun notifyExpenseCreated(
        groupId: Int,
        groupName: String,
        expenseDescription: String,
        amount: Double,
        createdByUsername: String,
        memberUserIds: List<Int>,
        excludeUserId: Int?
    ) {
        withContext(Dispatchers.IO) {
            try {
                var targetUsers = if (excludeUserId != null) {
                    memberUserIds.filter { it != excludeUserId }
                } else {
                    memberUserIds
                }

                // filter by notification preference
                targetUsers = filterNotificationPreference(groupId, targetUsers)

                val tokens = transaction {
                    userRepository.getFcmTokensForUsers(targetUsers)
                }

                if (tokens.isNotEmpty()) {
                    sendToTokens(
                        tokens = tokens,
                        title = "New expense in $groupName",
                        body = "$createdByUsername added: $expenseDescription (${formatAmount(amount)})",
                        data = mapOf(
                            "type" to "expense_created",
                            "groupId" to groupId.toString(),
                            "groupName" to groupName
                        )
                    )
                }
            } catch (e: Exception) {
                logger.error("Failed to send expense notification", e)
            }
        }
    }

    /**
     * Notify users about a completed settlement
     */

    suspend fun notifySettlementCompleted(
        groupId: Int,
        groupName: String,
        fromUsername: String,
        toUsername: String,
        amount: Double,
        notifyUserIds: List<Int>
    ) {
        withContext(Dispatchers.IO) {
            try {

                // filter by notification preference
                val filteredUsersIds = filterNotificationPreference(groupId, notifyUserIds)

                val tokens = transaction {
                    userRepository.getFcmTokensForUsers(filteredUsersIds)
                }

                if (tokens.isNotEmpty()) {
                    sendToTokens(
                        tokens = tokens,
                        title = "Settlement completed in $groupName",
                        body = "$fromUsername paid $toUsername ${formatAmount(amount)}",
                        data = mapOf(
                            "type" to "settlement_completed",
                            "groupId" to groupId.toString(),
                            "groupName" to groupName
                        )
                    )
                }
            } catch (e: Exception) {
                logger.error("Failed to send settlement notification", e)
            }
        }
    }

    /**
     * Notify user about a new chat message
     */

    suspend fun notifyNewMessage(
        groupId: Int,
        groupName: String,
        senderUsername: String,
        messagePreview: String,
        recipientUserId: Int
    ) {
        withContext(Dispatchers.IO) {
            try {
                val preview = if (messagePreview.length > 50) {
                    messagePreview.take(50) + "..."
                } else {
                    messagePreview
                }

                // filter by notification preference
                val recipients = filterNotificationPreference(groupId, listOf(recipientUserId))

                val tokens = transaction {
                    userRepository.getFcmTokensForUsers(recipients)
                }

                if (tokens.isNotEmpty()) {
                    sendToTokens(
                        tokens = tokens,
                        title = "$senderUsername in $groupName",
                        body = preview,
                        data = mapOf(
                            "type" to "message_received",
                            "groupId" to groupId.toString(),
                            "groupName" to groupName
                        )
                    )
                }
            } catch (e: Exception) {
                logger.error("Failed to send message notification", e)
            }
        }
    }

    /**
     * Notify user they were added to a group
     */
    suspend fun notifyAddedToGroup(
        groupId: Int,
        groupName: String,
        addedByUsername: String,
        newMemberUserId: Int
    ) {
        withContext(Dispatchers.IO) {
            try {

                // filter by notification preference
                val recipients = filterNotificationPreference(groupId, listOf(newMemberUserId))

                val tokens = transaction {
                    userRepository.getFcmTokensForUsers(recipients)
                }

                if (tokens.isNotEmpty()) {
                    sendToTokens(
                        tokens = tokens,
                        title = "Added to group",
                        body = "$addedByUsername added you to $groupName",
                        data = mapOf(
                            "type" to "added_to_group",
                            "groupId" to groupId.toString(),
                            "groupName" to groupName
                        )
                    )
                }
            } catch (e: Exception) {
                logger.error("Failed to send add member notification", e)
            }
        }
    }

    private fun formatAmount(amount: Double): String {
        return String.format("%.2f DKK", amount)
    }
}
