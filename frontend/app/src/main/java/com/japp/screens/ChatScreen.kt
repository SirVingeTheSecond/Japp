package com.japp.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.japp.api.CredentialsStorage
import com.japp.api.RetrofitClient
import com.japp.api.responses.WebSocketMessageType
import com.japp.api.responses.message.CreateMessageRequest
import com.japp.api.responses.message.MessageDto
import com.japp.composables.MessageBubble
import com.japp.composables.TypingIndicator
import com.japp.websocket.ChatWebSocketClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(groupId: Int) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var messages by remember { mutableStateOf<List<MessageDto>>(emptyList()) }
    var messageInput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isTyping by remember { mutableStateOf(false) }
    var typingJob by remember { mutableStateOf<Job?>(null) }
    var lastProcessedMessageCount by remember { mutableStateOf(0) }

    val credentials = CredentialsStorage.load(context)
    val currentUserId = credentials?.userId

    // Subscribe to group once WebSocket is connected
    val isConnected by ChatWebSocketClient.isConnected.collectAsState()

    // try to subscribe when screen opens
    LaunchedEffect(Unit) {
        Log.d("ChatScreen", "Screen opened, current isConnected: ${ChatWebSocketClient.isConnected.value}")
    }

    LaunchedEffect(groupId, isConnected) {
        Log.d("ChatScreen", "LaunchedEffect triggered - groupId: $groupId, isConnected: $isConnected")
        if (isConnected) {
            Log.d("ChatScreen", "WebSocket IS connected, subscribing to group $groupId")
            ChatWebSocketClient.subscribeToGroup(groupId)
        } else {
            Log.d("ChatScreen", "WebSocket NOT connected yet, waiting...")
        }
    }

    // Load message history
    LaunchedEffect(groupId) {
        try {
            val response = RetrofitClient.messageService.getGroupMessages(groupId)
            if (response.isSuccessful && response.body() != null) {
                messages = response.body()!!.messages.reversed() // Oldest first
                isLoading = false
            } else {
                errorMessage = "Failed to load messages"
                isLoading = false
            }
        } catch (e: Exception) {
            Log.e("ChatScreen", "Error loading messages", e)
            errorMessage = e.message
            isLoading = false
        }
    }

    // Listen for incoming WebSocket messages
    val wsMessages by ChatWebSocketClient.incomingMessages.collectAsState()
    LaunchedEffect(wsMessages.size) {
        if (wsMessages.size > lastProcessedMessageCount) {
            wsMessages.drop(lastProcessedMessageCount).forEach { wsMsg ->
                when (wsMsg.type) {
                    WebSocketMessageType.NEW_MESSAGE, WebSocketMessageType.MESSAGE_SENT -> {
                        wsMsg.message?.let { newMsg ->
                            if (newMsg.groupId == groupId && !messages.any { it.id == newMsg.id }) {
                                messages = messages + newMsg

                                // Auto-scroll if user is near bottom
                                if (listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index == messages.size - 2 ||
                                    messages.size == 1) {
                                    listState.animateScrollToItem(messages.size - 1)
                                }
                            }
                        }
                    }
                    WebSocketMessageType.SUBSCRIBED -> {
                        Log.d("ChatScreen", "Subscribed to group $groupId")
                    }
                    WebSocketMessageType.ERROR -> {
                        errorMessage = wsMsg.error
                    }
                    else -> {}
                }
            }
            lastProcessedMessageCount = wsMessages.size
        }
    }

    // Listen for typing indicators
    val typingUsers by ChatWebSocketClient.typingUsers.collectAsState()
    val currentlyTyping = typingUsers[groupId] ?: emptyList()

    // Cleanup
    DisposableEffect(groupId) {
        onDispose {
            if (isTyping) {
                ChatWebSocketClient.sendTypingStop(groupId)
            }
            ChatWebSocketClient.unsubscribeFromGroup(groupId)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            // Messages list
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    MessageBubble(
                        message = message,
                        currentUserId = currentUserId
                    )
                }
            }
        }

        // Typing indicator
        TypingIndicator(usernames = currentlyTyping)

        // Error message
        errorMessage?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        // Input
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = messageInput,
                    onValueChange = { newValue ->
                        messageInput = newValue

                        // Typing indicator
                        if (newValue.isNotBlank() && !isTyping) {
                            isTyping = true
                            ChatWebSocketClient.sendTypingStart(groupId)
                        }

                        // Cancel previous and start new one
                        typingJob?.cancel()
                        typingJob = scope.launch {
                            delay(2000) // 2 seconds of no typing
                            if (isTyping) {
                                isTyping = false
                                ChatWebSocketClient.sendTypingStop(groupId)
                            }
                        }
                    },
                    placeholder = {
                        Text(
                            "Message",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    modifier = Modifier.weight(1f),
                    maxLines = 4,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                        disabledIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    ),
                    textStyle = MaterialTheme.typography.bodyLarge
                )

                IconButton(
                    onClick = {
                        if (messageInput.isNotBlank()) {
                            // Stop typing indicator
                            typingJob?.cancel()
                            if (isTyping) {
                                isTyping = false
                                ChatWebSocketClient.sendTypingStop(groupId)
                            }

                            val messageContent = messageInput.trim()
                            messageInput = ""

                            ChatWebSocketClient.sendMessage(groupId, messageContent)
                        }
                    },
                    enabled = messageInput.isNotBlank()
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send message",
                        tint = if (messageInput.isNotBlank())
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
