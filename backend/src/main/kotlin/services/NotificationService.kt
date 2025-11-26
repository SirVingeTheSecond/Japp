package com.japp.services

import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import com.japp.services.interfaces.IUserRepository
import org.slf4j.LoggerFactory

/**
 * Service for sending push notifications via Firebase Cloud Messaging.
 */
// Could utilize org.slf4j.LoggerFactory for logging but I digress
class NotificationService(
    private val firebaseApp: FirebaseApp,
    private val userRepository: IUserRepository
) {
    private val logger = LoggerFactory.getLogger(NotificationService::class.java)

    /**
     * Send notification to a single user
     */
    suspend fun sendToUser(
        userId: Int,
        title: String,
        body: String,
        data: Map<String, String> = emptyMap()
    ): Boolean {
        return sendToUsers(listOf(userId), title, body, data)
    }

    /**
     * Send notification to multiple users
     */
    suspend fun sendToUsers(
        userIds: List<Int>,
        title: String,
        body: String,
        data: Map<String, String> = emptyMap()
    ): Boolean {
        if (userIds.isEmpty()) {
            logger.debug("No users to notify")
            return false
        }

        val tokens = userRepository.getFcmTokensForUsers(userIds)
        if (tokens.isEmpty()) {
            logger.debug("No FCM tokens found for users: $userIds")
            return false
        }

        return sendToTokens(tokens, title, body, data)
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

                // Invalid token - could queue for cleanup
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
        excludeUserId: Int? = null
    ) {
        val targetUsers = if (excludeUserId != null) {
            memberUserIds.filter { it != excludeUserId }
        } else {
            memberUserIds
        }

        sendToUsers(
            userIds = targetUsers,
            title = "New expense in $groupName",
            body = "$createdByUsername added: $expenseDescription (${formatAmount(amount)})",
            data = mapOf(
                "type" to "expense_created",
                "groupId" to groupId.toString(),
                "groupName" to groupName
            )
        )
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
        sendToUsers(
            userIds = notifyUserIds,
            title = "Settlement completed in $groupName",
            body = "$fromUsername paid $toUsername ${formatAmount(amount)}",
            data = mapOf(
                "type" to "settlement_completed",
                "groupId" to groupId.toString(),
                "groupName" to groupName
            )
        )
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
        val preview = if (messagePreview.length > 50) {
            messagePreview.take(50) + "..."
        } else {
            messagePreview
        }

        sendToUser(
            userId = recipientUserId,
            title = "$senderUsername in $groupName",
            body = preview,
            data = mapOf(
                "type" to "message_received",
                "groupId" to groupId.toString(),
                "groupName" to groupName
            )
        )
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
        sendToUser(
            userId = newMemberUserId,
            title = "Added to group",
            body = "$addedByUsername added you to $groupName",
            data = mapOf(
                "type" to "added_to_group",
                "groupId" to groupId.toString(),
                "groupName" to groupName
            )
        )
    }

    private fun formatAmount(amount: Double): String {
        return String.format("%.2f DKK", amount)
    }
}
