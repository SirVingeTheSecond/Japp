package com.japp.routes

import com.japp.models.Result
import com.japp.models.dto.CreateSettlementRequest
import com.japp.plugins.getUserId
import com.japp.services.SettlementService
import com.japp.utils.ResponseFactory
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.settlementRoutes() {
    val settlementService by inject<SettlementService>()

    route("/settlements") {

        post {
            val request = call.receive<CreateSettlementRequest>()
            val userId = call.getUserId()

            when (val result = settlementService.createSettlement(request, userId)) {
                is Result.Success -> {
                    call.respond(HttpStatusCode.Created, result.value)
                }
                is Result.Failure -> {
                    val status = HttpStatusCode.fromValue(result.error.httpStatus)
                    call.respond(
                        status,
                        ResponseFactory.error(
                            error = result.error::class.simpleName ?: "Error",
                            message = result.error.message
                        )
                    )
                }
            }
        }

        get("/group/{groupId}") {
            val groupId = call.parameters["groupId"]?.toIntOrNull()
                ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    ResponseFactory.error(
                        error = "ValidationError",
                        message = "Invalid group ID"
                    )
                )

            val pendingOnly = call.request.queryParameters["pending"]?.toBoolean() ?: false
            val userId = call.getUserId()

            when (val result = settlementService.getGroupSettlements(groupId, userId, pendingOnly)) {
                is Result.Success -> {
                    call.respond(HttpStatusCode.OK, result.value)
                }
                is Result.Failure -> {
                    val status = HttpStatusCode.fromValue(result.error.httpStatus)
                    call.respond(
                        status,
                        ResponseFactory.error(
                            error = result.error::class.simpleName ?: "Error",
                            message = result.error.message
                        )
                    )
                }
            }
        }

        get("/group/{groupId}/suggestions") {
            val groupId = call.parameters["groupId"]?.toIntOrNull()
                ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    ResponseFactory.error(
                        error = "ValidationError",
                        message = "Invalid group ID"
                    )
                )

            val userId = call.getUserId()

            when (val result = settlementService.getSettlementSuggestions(groupId, userId)) {
                is Result.Success -> {
                    call.respond(HttpStatusCode.OK, result.value)
                }
                is Result.Failure -> {
                    val status = HttpStatusCode.fromValue(result.error.httpStatus)
                    call.respond(
                        status,
                        ResponseFactory.error(
                            error = result.error::class.simpleName ?: "Error",
                            message = result.error.message
                        )
                    )
                }
            }
        }

        patch("/{id}/complete") {
            val settlementId = call.parameters["id"]?.toIntOrNull()
                ?: return@patch call.respond(
                    HttpStatusCode.BadRequest,
                    ResponseFactory.error(
                        error = "ValidationError",
                        message = "Invalid settlement ID"
                    )
                )

            val userId = call.getUserId()

            when (val result = settlementService.markSettlementCompleted(settlementId, userId)) {
                is Result.Success -> {
                    call.respond(HttpStatusCode.OK, result.value)
                }
                is Result.Failure -> {
                    val status = HttpStatusCode.fromValue(result.error.httpStatus)
                    call.respond(
                        status,
                        ResponseFactory.error(
                            error = result.error::class.simpleName ?: "Error",
                            message = result.error.message
                        )
                    )
                }
            }
        }
    }
}