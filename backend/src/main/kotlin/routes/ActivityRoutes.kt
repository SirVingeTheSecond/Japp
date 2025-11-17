package com.japp.routes

import com.japp.plugins.getUserId
import com.japp.repositories.interfaces.IGroupRepository
import com.japp.services.ActivityService
import com.japp.utils.ResponseFactory
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.ktor.ext.inject
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

fun Route.activityRoutes() {
    val activityService by inject<ActivityService>()
    val groupRepository by inject<IGroupRepository>()

    route("/activities") {

        // Get activities for current user across all groups
        get {
            val userId = call.getUserId()
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50

            try {
                val activities = withContext(Dispatchers.IO) {
                    transaction {
                        activityService.getUserActivities(userId, limit)
                    }
                }

                call.respond(HttpStatusCode.OK, activities)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ResponseFactory.error(
                        error = "InternalServerError",
                        message = e.message ?: "Failed to retrieve activities"
                    )
                )
            }
        }

        // Get activities for a specific group
        get("/group/{groupId}") {
            val groupId = call.parameters["groupId"]?.toIntOrNull()
                ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    ResponseFactory.error(
                        error = "ValidationError",
                        message = "Invalid group ID"
                    )
                )

            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50
            val userId = call.getUserId()

            try {
                val activities = withContext(Dispatchers.IO) {
                    transaction {
                        if (!groupRepository.isMember(groupId, userId)) {
                            return@transaction null
                        }
                        activityService.getGroupActivities(groupId, limit)
                    }
                }

                if (activities == null) {
                    call.respond(
                        HttpStatusCode.Forbidden,
                        ResponseFactory.error(
                            error = "Forbidden",
                            message = "Not a member of this group"
                        )
                    )
                } else {
                    call.respond(HttpStatusCode.OK, activities)
                }

            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ResponseFactory.error(
                        error = "InternalServerError",
                        message = e.message ?: "Failed to retrieve activities"
                    )
                )
            }
        }
    }
}