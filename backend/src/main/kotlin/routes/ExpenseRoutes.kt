package com.japp.routes

import com.japp.models.Result
import com.japp.models.dto.CreateExpenseRequest
import com.japp.plugins.getUserId
import com.japp.services.ExpenseService
import com.japp.utils.ResponseFactory
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.expenseRoutes() {
    val expenseService by inject<ExpenseService>()

    route("/expenses") {

        post {
            val request = call.receive<CreateExpenseRequest>()
            val userId = call.getUserId()

            when (val result = expenseService.createExpense(request, userId)) {
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

            val userId = call.getUserId()

            when (val result = expenseService.getGroupExpenses(groupId, userId)) {
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

        get("/group/{groupId}/balances") {
            val groupId = call.parameters["groupId"]?.toIntOrNull()
                ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    ResponseFactory.error(
                        error = "ValidationError",
                        message = "Invalid group ID"
                    )
                )

            val userId = call.getUserId()

            when (val result = expenseService.getGroupBalances(groupId, userId)) {
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

        delete("/{id}") {
            val expenseId = call.parameters["id"]?.toIntOrNull()
                ?: return@delete call.respond(
                    HttpStatusCode.BadRequest,
                    ResponseFactory.error(
                        error = "ValidationError",
                        message = "Invalid expense ID"
                    )
                )

            val userId = call.getUserId()

            when (val result = expenseService.deleteExpense(expenseId, userId)) {
                is Result.Success -> {
                    call.respond(
                        HttpStatusCode.OK,
                        ResponseFactory.success("Expense deleted successfully")
                    )
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