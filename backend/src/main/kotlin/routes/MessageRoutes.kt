package com.japp.routes

import com.japp.models.WebSocketMessageType
import com.japp.models.dto.CreateMessageRequest
import com.japp.models.dto.MarkMessageReadRequest
import com.japp.models.dto.WebSocketMessage
import com.japp.plugins.getUserId
import com.japp.services.ChatWebSocketService
import com.japp.services.MessageService
import com.japp.utils.requirePathInt
import com.japp.utils.requireQueryInt
import com.japp.utils.respondResult
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.json.Json
import org.koin.ktor.ext.inject

fun Route.messageRoutes() {
    val messageService by inject<MessageService>()

    route("/messages") {
        // Fallback if WebSocket disconnects, although I'm not very fond of it being seen as backwards compatibility
        post {
            val request = call.receive<CreateMessageRequest>()
            val userId = call.getUserId()
            val result = messageService.createMessage(request, userId)
            call.respondResult(result, HttpStatusCode.Created)
        }

        get("/group/{groupId}") {
            val groupId = call.requirePathInt("groupId")
            val userId = call.getUserId()
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50
            val beforeCursor = call.request.queryParameters["before"]

            val result = messageService.getMessages(groupId, userId, limit, beforeCursor)
            call.respondResult(result)
        }

        post("/read") {
            val request = call.receive<MarkMessageReadRequest>()
            val userId = call.getUserId()
            val groupId = call.requireQueryInt("groupId")
            val result = messageService.markMessagesAsRead(request.messageIds, userId, groupId)
            call.respondResult(result)
        }

        delete("/{id}") {
            val messageId = call.requirePathInt("id")
            val userId = call.getUserId()
            val result = messageService.deleteMessage(messageId, userId)
            call.respondResult(result)
        }
    }
}

fun Route.chatWebSocket() {
    val chatService by inject<ChatWebSocketService>()

    webSocket("/ws/chat") {
        val userId = try {
            call.getUserId()
        } catch (e: Exception) {
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Unauthorized"))
            return@webSocket
        }

        try {
            // connection confirmation
            val connectMessage = chatService.handleConnect(userId, this)
            send(Frame.Text(Json.encodeToString(WebSocketMessage.serializer(), connectMessage)))

            val heartbeatJob = chatService.startHeartbeat(this, userId)

            // Process incoming messages
            for (frame in incoming) {
                when (frame) {
                    is Frame.Text -> {
                        val text = frame.readText()
                        try {
                            val incomingMessage = Json.decodeFromString<WebSocketMessage>(text)
                            val responseMessage = chatService.handleMessage(incomingMessage, userId, this)

                            // Send response if service returned one
                            responseMessage?.let {
                                send(Frame.Text(Json.encodeToString(WebSocketMessage.serializer(), it)))
                            }
                        } catch (_: Exception) {
                            send(Frame.Text(Json.encodeToString(
                                WebSocketMessage.serializer(),
                                WebSocketMessage(
                                    type = WebSocketMessageType.ERROR,
                                    error = "invalid_message_format"
                                )
                            )))
                        }
                    }
                    else -> {}
                }
            }

            heartbeatJob.cancel()
        } catch (_: Exception) {
            // Connection error - cleanup handled in finally
        } finally {
            chatService.handleDisconnect(this)
        }
    }
}