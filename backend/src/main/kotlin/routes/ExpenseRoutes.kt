package com.japp.routes

import com.japp.models.dto.CreateExpenseRequest
import com.japp.plugins.getUserId
import com.japp.services.ExpenseService
import com.japp.utils.requirePathInt
import com.japp.utils.respondResult
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.expenseRoutes() {
    val expenseService by inject<ExpenseService>()

    route("/expenses") {

        post {
            val request = call.receive<CreateExpenseRequest>()
            val userId = call.getUserId()
            val result = expenseService.createExpense(request, userId)
            call.respondResult(result, HttpStatusCode.Created)
        }

        get("/group/{groupId}") {
            val groupId = call.requirePathInt("groupId")
            val userId = call.getUserId()
            val result = expenseService.getGroupExpenses(groupId, userId)
            call.respondResult(result)
        }

        get("/group/{groupId}/balances") {
            val groupId = call.requirePathInt("groupId")
            val userId = call.getUserId()
            val result = expenseService.getGroupBalances(groupId, userId)
            call.respondResult(result)
        }

        delete("/{id}") {
            val expenseId = call.requirePathInt("id")
            val userId = call.getUserId()
            val result = expenseService.deleteExpense(expenseId, userId)
            call.respondResult(result)
        }
    }
}