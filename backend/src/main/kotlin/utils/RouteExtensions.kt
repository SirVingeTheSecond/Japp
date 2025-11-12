package com.japp.utils

import com.japp.models.IAppError
import com.japp.models.Result
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*

/**
 * Respond with Result, automatically mapping AppError to HTTP responses
 */
suspend inline fun <reified T : Any> ApplicationCall.respondResult(
    result: Result<T, IAppError>,
    successStatus: HttpStatusCode = HttpStatusCode.OK
) {
    when (result) {
        is Result.Success -> {
            respond(successStatus, result.value)
        }
        is Result.Failure -> {
            val status = HttpStatusCode.fromValue(result.error.httpStatus)
            respond(
                status,
                ResponseFactory.error(
                    error = result.error::class.simpleName ?: "Error",
                    message = result.error.message
                )
            )
        }
    }
}