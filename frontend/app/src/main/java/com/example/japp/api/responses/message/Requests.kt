package com.example.japp.api.responses.message

data class CreateMessageRequest(
    val groupId: Int,
    val content: String
)

data class MarkMessageReadRequest(
    val messageIds: List<Int>
)