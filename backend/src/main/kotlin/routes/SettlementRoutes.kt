package com.japp.routes

import com.japp.models.dto.CreateSettlementRequest
import com.japp.plugins.getUserId
import com.japp.services.SettlementService
import com.japp.utils.getQueryBoolean
import com.japp.utils.requirePathInt
import com.japp.utils.respondResult
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.settlementRoutes() {
    val settlementService by inject<SettlementService>()

    route("/settlements") {

        post {
            val request = call.receive<CreateSettlementRequest>()
            val userId = call.getUserId()
            val result = settlementService.createSettlement(request, userId)
            call.respondResult(result, HttpStatusCode.Created)
        }

        get("/group/{groupId}") {
            val groupId = call.requirePathInt("groupId")
            val pendingOnly = call.getQueryBoolean("pending", default = false)
            val userId = call.getUserId()
            val result = settlementService.getGroupSettlements(groupId, userId, pendingOnly)
            call.respondResult(result)
        }

        get("/group/{groupId}/suggestions") {
            val groupId = call.requirePathInt("groupId")
            val userId = call.getUserId()
            val result = settlementService.getSettlementSuggestions(groupId, userId)
            call.respondResult(result)
        }

        patch("/{id}/complete") {
            val settlementId = call.requirePathInt("id")
            val userId = call.getUserId()
            val result = settlementService.markSettlementCompleted(settlementId, userId)
            call.respondResult(result)
        }
    }
}