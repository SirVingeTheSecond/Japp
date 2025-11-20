package com.japp.routes

import com.japp.models.Result
import com.japp.models.WebSocketMessageType
import com.japp.models.dto.CreateMessageRequest
import com.japp.models.dto.MarkMessageReadRequest
import com.japp.models.dto.WebSocketMessage
import com.japp.plugins.getUserId
import com.japp.services.MessageService
import com.japp.utils.requirePathInt
import com.japp.utils.requireQueryInt
import com.japp.utils.respondResult
import com.japp.websocket.WebSocketManager
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
    val messageService by inject<MessageService>()
    val webSocketManager by inject<WebSocketManager>()

    webSocket("/ws/chat") {
        val userId = try {
            call.getUserId()
        } catch (e: Exception) {
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Unauthorized"))
            return@webSocket
        }

        webSocketManager.registerSession(userId, this)

        try {
            send(Frame.Text(Json.encodeToString(
                WebSocketMessage.serializer(),
                WebSocketMessage(
                    type = WebSocketMessageType.CONNECTED,
                    userId = userId
                )
            )))

            val heartbeatJob = webSocketManager.startHeartbeat(this, userId)

            for (frame in incoming) {
                when (frame) {
                    is Frame.Text -> {
                        val text = frame.readText()
                        try {
                            val message = Json.decodeFromString<WebSocketMessage>(text)

                            when (message.type) {
                                WebSocketMessageType.SUBSCRIBE -> {
                                    val groupId = message.groupId
                                    if (groupId == null) {
                                        send(Frame.Text(Json.encodeToString(
                                            WebSocketMessage.serializer(),
                                            WebSocketMessage(
                                                type = WebSocketMessageType.ERROR,
                                                error = "groupId required for subscribe"
                                            )
                                        )))
                                        continue
                                    }

                                    val result = messageService.subscribeToGroup(groupId, userId, this)

                                    when (result) {
                                        is Result.Success -> {
                                            send(Frame.Text(Json.encodeToString(
                                                WebSocketMessage.serializer(),
                                                WebSocketMessage(
                                                    type = WebSocketMessageType.SUBSCRIBED,
                                                    groupId = groupId
                                                )
                                            )))
                                        }
                                        is Result.Failure -> {
                                            send(Frame.Text(Json.encodeToString(
                                                WebSocketMessage.serializer(),
                                                WebSocketMessage(
                                                    type = WebSocketMessageType.ERROR,
                                                    groupId = groupId,
                                                    error = result.error.message
                                                )
                                            )))
                                        }
                                    }
                                }

                                WebSocketMessageType.UNSUBSCRIBE -> {
                                    val groupId = message.groupId
                                    if (groupId == null) {
                                        send(Frame.Text(Json.encodeToString(
                                            WebSocketMessage.serializer(),
                                            WebSocketMessage(
                                                type = WebSocketMessageType.ERROR,
                                                error = "groupId required for unsubscribe"
                                            )
                                        )))
                                        continue
                                    }

                                    messageService.unsubscribeFromGroup(groupId, this)

                                    send(Frame.Text(Json.encodeToString(
                                        WebSocketMessage.serializer(),
                                        WebSocketMessage(
                                            type = WebSocketMessageType.UNSUBSCRIBED,
                                            groupId = groupId
                                        )
                                    )))
                                }

                                WebSocketMessageType.TYPING_START, WebSocketMessageType.TYPING_STOP -> {
                                    val groupId = message.groupId
                                    if (groupId != null && webSocketManager.isSubscribed(this, groupId)) {
                                        webSocketManager.broadcastToGroup(
                                            groupId = groupId,
                                            message = message.copy(userId = userId),
                                            excludeUserId = userId
                                        )
                                    }
                                }

                                WebSocketMessageType.PING -> {
                                    send(Frame.Text(Json.encodeToString(
                                        WebSocketMessage.serializer(),
                                        WebSocketMessage(type = WebSocketMessageType.PONG)
                                    )))
                                }

                                else -> {
                                    // Ignore other message types (NEW_MESSAGE, MESSAGE_READ, etc. are server -> client only)
                                }
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
            webSocketManager.unregisterSession(this)
        }
    }
}