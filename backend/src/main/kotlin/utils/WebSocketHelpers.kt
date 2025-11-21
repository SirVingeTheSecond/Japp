package com.japp.utils

import com.japp.models.Result
import com.japp.models.WebSocketMessageType
import com.japp.models.dto.MessageDto
import com.japp.models.dto.WebSocketMessage
import com.japp.models.error.AppError

/**
 * Convert Result to WebSocketMessage.
 */
fun Result<MessageDto, AppError>.toWebSocketMessage(): WebSocketMessage {
    return when (this) {
        is Result.Success -> WebSocketMessage(
            type = WebSocketMessageType.MESSAGE_SENT,
            message = this.value
        )
        is Result.Failure -> WebSocketMessage(
            type = WebSocketMessageType.ERROR,
            error = this.error.message
        )
    }
}