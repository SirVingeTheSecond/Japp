package com.japp.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.japp.api.CredentialsStorage
import com.japp.api.NetworkResult
import com.japp.api.RetrofitClient
import com.japp.api.responses.WebSocketMessageType
import com.japp.api.responses.message.MessageDto
import com.japp.api.safeApiCall
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

    // Message state
    var messages by remember { mutableStateOf<List<MessageDto>>(emptyList()) }
    var messageInput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Typing indicator state
    var isTyping by remember { mutableStateOf(false) }
    var typingJob by remember { mutableStateOf<Job?>(null) }

    // Track WebSocket messages to avoid reprocessing
    var lastProcessedMessageCount by remember { mutableIntStateOf(0) }

    val credentials = CredentialsStorage.load(context)
    val currentUserId = credentials?.userId

    // Subscribe to group chat once WebSocket is connected
    val isConnected by ChatWebSocketClient.isConnected.collectAsState()
    LaunchedEffect(groupId, isConnected) {
        if (isConnected) {
            ChatWebSocketClient.subscribeToGroup(groupId)
        }
    }

    // Message history
    LaunchedEffect(groupId) {
        isLoading = true
        errorMessage = null

        when (val result = safeApiCall("ChatScreen.messages") {
            RetrofitClient.messageService.getGroupMessages(groupId)
        }) {
            is NetworkResult.Success -> {
                messages = result.data.messages.reversed() // Oldest first
                isLoading = false
            }
            is NetworkResult.Error -> {
                errorMessage = result.message
                isLoading = false
            }
        }
    }

    // Auto scroll to latest message after initial load
    LaunchedEffect(isLoading, messages.size) {
        if (!isLoading && messages.isNotEmpty()) {
            listState.scrollToItem(messages.size - 1)
        }
    }

    val wsMessages by ChatWebSocketClient.incomingMessages.collectAsState()
    LaunchedEffect(wsMessages.size) {
        if (wsMessages.size > lastProcessedMessageCount) {
            // Process only new messages since last update
            wsMessages.drop(lastProcessedMessageCount).forEach { wsMsg ->
                when (wsMsg.type) {
                    WebSocketMessageType.NEW_MESSAGE, WebSocketMessageType.MESSAGE_SENT -> {
                        wsMsg.message?.let { newMsg ->
                            if (newMsg.groupId == groupId && !messages.any { it.id == newMsg.id }) {
                                messages = messages + newMsg

                                // Auto scroll if user is viewing the bottom of the chat
                                if (listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index == messages.size - 2 ||
                                    messages.size == 1) {
                                    listState.animateScrollToItem(messages.size - 1)
                                }
                            }
                        }
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

    // Listen for typing indicators from other users
    val typingUsers by ChatWebSocketClient.typingUsers.collectAsState()
    val currentlyTyping = typingUsers[groupId] ?: emptyList()

    // unsubscribe and stop typing indicator when leaving screen
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

        // Typing indicator for other users
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

        // Message input
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

                        // Send typing indicator when user starts typing
                        if (newValue.isNotBlank() && !isTyping) {
                            isTyping = true
                            ChatWebSocketClient.sendTypingStart(groupId)
                        }

                        // Stop typing indicator after 2 seconds of no input
                        typingJob?.cancel()
                        typingJob = scope.launch {
                            delay(2000)
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

                            // Send message via WebSocket
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
