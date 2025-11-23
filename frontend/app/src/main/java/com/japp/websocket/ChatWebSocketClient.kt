package com.japp.websocket

import android.util.Log
import com.google.gson.*
import com.japp.api.responses.MessageType
import com.japp.api.responses.WebSocketMessageType
import com.japp.api.responses.message.WebSocketMessageDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.*
import java.lang.reflect.Type

class WebSocketMessageTypeDeserializer : JsonDeserializer<WebSocketMessageType> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): WebSocketMessageType? {
        val value = json.asString
        return WebSocketMessageType.fromString(value)
    }
}

class WebSocketMessageTypeSerializer : JsonSerializer<WebSocketMessageType> {
    override fun serialize(
        src: WebSocketMessageType,
        typeOfSrc: Type,
        context: JsonSerializationContext
    ): JsonElement {
        return JsonPrimitive(src.value)
    }
}

class MessageTypeSerializer : JsonSerializer<MessageType> {
    override fun serialize(
        src: MessageType,
        typeOfSrc: Type,
        context: JsonSerializationContext
    ): JsonElement {
        return JsonPrimitive(src.value)
    }
}

object ChatWebSocketClient {
    private const val TAG = "ChatWS"
    // Android emulator
    //private const val WS_URL = "ws://10.0.2.2:8080/api/ws/chat"
    // "Production"
    private const val WS_URL = "wss://japp-app-api.itnerd.net/api/ws/chat"

    private var webSocket: WebSocket? = null
    private val client = OkHttpClient()

    // enum serializers for backend compatibility
    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(WebSocketMessageType::class.java, WebSocketMessageTypeDeserializer())
        .registerTypeAdapter(WebSocketMessageType::class.java, WebSocketMessageTypeSerializer())
        .registerTypeAdapter(MessageType::class.java, MessageTypeSerializer())
        .create()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    private val _incomingMessages = MutableStateFlow<List<WebSocketMessageDto>>(emptyList())
    val incomingMessages: StateFlow<List<WebSocketMessageDto>> = _incomingMessages

    private val _typingUsers = MutableStateFlow<Map<Int, List<String>>>(emptyMap())
    val typingUsers: StateFlow<Map<Int, List<String>>> = _typingUsers

    // access token for reconnection attempts
    private var accessToken: String? = null

    fun connect(token: String) {
        accessToken = token

        if (_isConnected.value) {
            return
        }

        val request = Request.Builder()
            .url(WS_URL)
            .addHeader("Authorization", "Bearer $token")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                // Update state on main thread to ensure UI observes change
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    _isConnected.value = true
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val msg = gson.fromJson(text, WebSocketMessageDto::class.java)

                    // Respond to PING immediately to keep connection alive
                    if (msg.type == WebSocketMessageType.PING) {
                        val pong = WebSocketMessageDto(type = WebSocketMessageType.PONG)
                        send(pong)
                        return
                    }

                    // Update typing indicator state
                    when (msg.type) {
                        WebSocketMessageType.TYPING_START -> {
                            msg.groupId?.let { groupId ->
                                msg.username?.let { username ->
                                    val current = _typingUsers.value[groupId] ?: emptyList()
                                    if (!current.contains(username)) {
                                        _typingUsers.value += (groupId to (current + username))
                                    }
                                }
                            }
                        }
                        WebSocketMessageType.TYPING_STOP -> {
                            msg.groupId?.let { groupId ->
                                msg.username?.let { username ->
                                    val current = _typingUsers.value[groupId] ?: emptyList()
                                    _typingUsers.value += (groupId to current.filter { it != username })
                                }
                            }
                        }
                        else -> {}
                    }

                    // Append to list (for UI)
                    _incomingMessages.value += msg
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse message", e)
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
                _isConnected.value = false
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket error", t)
                _isConnected.value = false

                // Auto reconnect after 3 seconds
                accessToken?.let { token ->
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        connect(token)
                    }, 3000)
                }
            }
        })
    }

    private fun send(message: WebSocketMessageDto) {
        val text = gson.toJson(message)
        val success = webSocket?.send(text) ?: false
        if (!success) {
            Log.e(TAG, "Failed to send message: WebSocket is ${if (webSocket == null) "null" else "closed"}")
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

    fun sendMessage(groupId: Int, content: String) {
        send(WebSocketMessageDto(
            type = WebSocketMessageType.NEW_MESSAGE,
            groupId = groupId,
            content = content
        ))
    }

    fun disconnect() {
        webSocket?.close(1000, "User disconnect")
        webSocket = null
        _isConnected.value = false
        _incomingMessages.value = emptyList()
        _typingUsers.value = emptyMap()
        accessToken = null
    }
}