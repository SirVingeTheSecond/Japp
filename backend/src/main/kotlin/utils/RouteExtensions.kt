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

/**
 * Extract and validate integer path parameter
 * Throws IllegalArgumentException if invalid (caught by StatusPages)
 */
fun ApplicationCall.requirePathInt(name: String): Int {
    return parameters[name]?.toIntOrNull()
        ?: throw IllegalArgumentException("Invalid $name parameter")
}

/**
 * Extract optional query parameter as boolean
 */
fun ApplicationCall.getQueryBoolean(name: String, default: Boolean = false): Boolean {
    return request.queryParameters[name]?.toBoolean() ?: default
}

/**
 * Extract optional query parameter as integer
 */
fun ApplicationCall.getQueryInt(name: String, default: Int? = null): Int? {
    return request.queryParameters[name]?.toIntOrNull() ?: default
}