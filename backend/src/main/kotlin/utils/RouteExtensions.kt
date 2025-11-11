package com.japp.utils

import com.japp.models.AuthError
import com.japp.models.Result
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*

/**
 * Respond with Result and auto mapping to HTTP responses
 */
suspend inline fun <reified T : Any, E : AuthError> ApplicationCall.respondResult(
    result: Result<T, E>,
    successStatus: HttpStatusCode = HttpStatusCode.OK
) {
    when (result) {
        is Result.Success -> respond(successStatus, result.value)
        is Result.Failure -> {
            val status = HttpStatusCode.fromValue(result.error.httpStatus)
            val errorResponse = ResponseFactory.error(
                error = result.error::class.simpleName ?: "Error",
                message = result.error.message
            )
            respond(status, errorResponse)
        }
    }
}