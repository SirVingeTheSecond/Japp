package com.japp.routes

import com.japp.plugins.getUserId
import com.japp.services.ActivityService
import com.japp.utils.requirePathInt
import com.japp.utils.respondResult
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.activityRoutes() {
    val activityService by inject<ActivityService>()

    route("/activities") {

        get {
            val userId = call.getUserId()
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50

            val result = activityService.getUserActivities(userId, limit)
            call.respondResult(result)
        }

        get("/group/{groupId}") {
            val groupId = call.requirePathInt("groupId")
            val userId = call.getUserId()
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50

            val result = activityService.getGroupActivities(groupId, userId, limit)
            call.respondResult(result)
        }
    }
}