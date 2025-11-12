package com.japp.utils

import com.japp.models.dto.ErrorResponse
import com.japp.models.dto.SuccessResponse

/**
 * Factory for creating API responses
 */
object ResponseFactory {

    fun error(
        error: String,
        message: String
    ): ErrorResponse = ErrorResponse(
        error = error,
        message = message
    )

    fun success(message: String): SuccessResponse = SuccessResponse(
        message = message
    )
}