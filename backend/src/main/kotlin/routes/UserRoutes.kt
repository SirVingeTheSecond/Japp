package com.japp.routes

import com.japp.models.dto.FcmTokenRequest
import com.japp.models.dto.UpdateUserRequest
import com.japp.plugins.getUserId
import com.japp.services.UserService
import com.japp.utils.respondResult
import io.ktor.server.request.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.userRoutes() {
    val userService by inject<UserService>()

    route("/user") {

        get("/me") {
            val userId = call.getUserId()
            val result = userService.getUserProfile(userId)
            call.respondResult(result)
        }

        patch("/me") {
            val userId = call.getUserId()
            val request = call.receive<UpdateUserRequest>()
            val result = userService.updateProfile(userId, request)
            call.respondResult(result)
        }

        post("/me/fcm-token") {
            val userId = call.getUserId()
            val request = call.receive<FcmTokenRequest>()
            val result = userService.updateFcmToken(userId, request.token)
            call.respondResult(result)
        }

        delete("/me/fcm-token") {
            val userId = call.getUserId()
            val result = userService.clearFcmToken(userId)
            call.respondResult(result)
        }

        // For viewing other user profiles
        get("/{id}") {
            val targetUserId = call.parameters["id"]?.toIntOrNull()
                ?: throw IllegalArgumentException("Invalid user ID")

            val result = userService.getUserProfile(targetUserId)
            call.respondResult(result)
        }
    }
}
