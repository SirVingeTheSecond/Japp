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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
            // Connection confirmation
            val connectMessage = chatService.handleConnect(userId, this)
            send(Frame.Text(Json.encodeToString(WebSocketMessage.serializer(), connectMessage)))

            // Start heartbeat job
            val heartbeatJob = chatService.startHeartbeat(this, userId)

            // Process incoming messages
            for (frame in incoming) {
                when (frame) {
                    is Frame.Text -> {
                        val text = frame.readText()

                        // async to avoid blocking messages
                        launch(Dispatchers.Default) {
                            try {
                                val incomingMessage = Json.decodeFromString<WebSocketMessage>(text)
                                println("Received WS message: type=${incomingMessage.type}, groupId=${incomingMessage.groupId}")

                                val responseMessage = chatService.handleMessage(incomingMessage, userId, this@webSocket)

                                // Send response if service returned one
                                responseMessage?.let {
                                    println("Sending WS response: type=${it.type}")
                                    send(Frame.Text(Json.encodeToString(WebSocketMessage.serializer(), it)))
                                }
                            } catch (e: Exception) {
                                println("ERROR processing WebSocket message: ${e.message}")
                                e.printStackTrace()

                                send(Frame.Text(Json.encodeToString(
                                    WebSocketMessage.serializer(),
                                    WebSocketMessage(
                                        type = WebSocketMessageType.ERROR,
                                        error = "Failed to process message: ${e.message}"
                                    )
                                )))
                            }
                        }
                    }
                    else -> {}
                }
            }

            heartbeatJob?.cancel()
        } catch (e: Exception) {
            println("WebSocket error for user $userId: ${e.message}")
            e.printStackTrace()
        } finally {
            chatService.handleDisconnect(this)
        }
    }
}
