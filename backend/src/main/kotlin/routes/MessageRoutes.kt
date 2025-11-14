package com.japp.routes

import com.japp.models.dto.CreateMessageRequest
import com.japp.models.dto.MarkMessageReadRequest
import com.japp.models.dto.WebSocketMessage
import com.japp.plugins.getUserId
import com.japp.services.MessageService
import com.japp.utils.requirePathInt
import com.japp.utils.respondResult
import com.japp.utils.ResponseFactory
import com.japp.websocket.WebSocketManager
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
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

            val groupId = call.request.queryParameters["groupId"]?.toIntOrNull()
                ?: return@post call.respond(
                    HttpStatusCode.BadRequest,
                    ResponseFactory.error(
                        error = "ValidationError",
                        message = "groupId query parameter is required"
                    )
                )

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
    val webSocketManager by inject<WebSocketManager>()

    webSocket("/ws/chat/{groupId}") {
        val groupIdParam = call.parameters["groupId"]
        val userId = try {
            call.getUserId()
        } catch (e: Exception) {
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Unauthorized"))
            return@webSocket
        }

        val groupId = groupIdParam?.toIntOrNull()
        if (groupId == null) {
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Invalid group ID"))
            return@webSocket
        }

        webSocketManager.registerConnection(groupId, userId, this)

        try {
            send(Frame.Text(Json.encodeToString(
                WebSocketMessage(
                    type = "connected",
                    groupId = groupId,
                    userId = userId
                )
            )))

            for (frame in incoming) {
                when (frame) {
                    is Frame.Text -> {
                        val text = frame.readText()
                        try {
                            val message = Json.decodeFromString<WebSocketMessage>(text)

                            when (message.type) {
                                "typing_start", "typing_stop" -> {
                                    webSocketManager.broadcastToGroup(
                                        groupId = groupId,
                                        message = message.copy(userId = userId),
                                        excludeUserId = userId
                                    )
                                }
                                "ping" -> {
                                    send(Frame.Text(Json.encodeToString(
                                        WebSocketMessage(
                                            type = "pong",
                                            groupId = groupId
                                        )
                                    )))
                                }
                            }
                        } catch (_: Exception) {
                            // Invalid message format
                        }
                    }
                    else -> {}
                }
            }
        } catch (_: Exception) {
            // Connection error
        } finally {
            webSocketManager.unregisterConnection(groupId, userId, this)
        }
    }
}