package com.japp.websocket

import android.util.Log
import com.google.gson.Gson
import com.japp.api.responses.WebSocketMessageType
import com.japp.api.responses.message.WebSocketMessageDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.*

object ChatWebSocketClient {
    private const val TAG = "ChatWS"
    private const val WS_URL = "wss://japp-app-api.itnerd.net/api/ws/chat"

    private var webSocket: WebSocket? = null
    private val client = OkHttpClient()
    private val gson = Gson()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    private val _incomingMessages = MutableStateFlow<List<WebSocketMessageDto>>(emptyList())
    val incomingMessages: StateFlow<List<WebSocketMessageDto>> = _incomingMessages

    // Map of groupId to list of usernames currently typing
    private val _typingUsers = MutableStateFlow<Map<Int, List<String>>>(emptyMap())
    val typingUsers: StateFlow<Map<Int, List<String>>> = _typingUsers

    fun connect(accessToken: String) {
        if (_isConnected.value) {
            Log.d(TAG, "Already connected")
            return
        }

        val request = Request.Builder()
            .url(WS_URL)
            .addHeader("Authorization", "Bearer $accessToken")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket connected")
                _isConnected.value = true
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val msg = gson.fromJson(text, WebSocketMessageDto::class.java)
                    Log.d(TAG, "Received: ${msg.type}")

                    // Handle typing indicators
                    when (msg.type) {
                        WebSocketMessageType.TYPING_START -> {
                            msg.groupId?.let { groupId ->
                                msg.username?.let { username ->
                                    val current = _typingUsers.value[groupId] ?: emptyList()
                                    if (!current.contains(username)) {
                                        _typingUsers.value = _typingUsers.value +
                                                (groupId to (current + username))
                                    }
                                }
                            }
                        }
                        WebSocketMessageType.TYPING_STOP -> {
                            msg.groupId?.let { groupId ->
                                msg.username?.let { username ->
                                    val current = _typingUsers.value[groupId] ?: emptyList()
                                    _typingUsers.value = _typingUsers.value +
                                            (groupId to current.filter { it != username })
                                }
                            }
                        }
                        else -> {}
                    }

                    _incomingMessages.value = _incomingMessages.value + msg
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse message", e)
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closing: $code - $reason")
                webSocket.close(1000, null)
                _isConnected.value = false
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket error", t)
                _isConnected.value = false
            }
        })
    }

    fun send(message: WebSocketMessageDto) {
        val text = gson.toJson(message)
        val success = webSocket?.send(text) ?: false
        if (!success) {
            Log.e(TAG, "Failed to send message")
        }
    }

    fun subscribeToGroup(groupId: Int) {
        send(WebSocketMessageDto(
            type = WebSocketMessageType.SUBSCRIBE,
            groupId = groupId
        ))
    }

    fun unsubscribeFromGroup(groupId: Int) {
        send(WebSocketMessageDto(
            type = WebSocketMessageType.UNSUBSCRIBE,
            groupId = groupId
        ))
    }

    fun sendTypingStart(groupId: Int) {
        send(WebSocketMessageDto(
            type = WebSocketMessageType.TYPING_START,
            groupId = groupId
        ))
    }

    fun sendTypingStop(groupId: Int) {
        send(WebSocketMessageDto(
            type = WebSocketMessageType.TYPING_STOP,
            groupId = groupId
        ))
    }

    fun disconnect() {
        webSocket?.close(1000, "User disconnect")
        webSocket = null
        _isConnected.value = false
        _incomingMessages.value = emptyList()
        _typingUsers.value = emptyMap()
        Log.d(TAG, "Disconnected")
    }
}