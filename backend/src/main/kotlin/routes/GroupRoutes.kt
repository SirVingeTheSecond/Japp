package com.japp.routes

import com.japp.models.Result
import com.japp.models.dto.CreateGroupRequest
import com.japp.models.dto.JoinGroupRequest
import com.japp.plugins.getUserId
import com.japp.services.GroupService
import com.japp.utils.ResponseFactory
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.groupRoutes() {
    val groupService by inject<GroupService>()

    route("/groups") {

        // Create a new group
        post {
            val request = call.receive<CreateGroupRequest>()
            val userId = call.getUserId()

            when (val result = groupService.createGroup(request, userId)) {
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

        // Get all groups for current user
        get {
            val userId = call.getUserId()

            when (val result = groupService.getUserGroups(userId)) {
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

        // Join a group via invite code
        // ToDo: Same endpoint for invite code and QR code? Same code but different representations.
        post("/join") {
            val request = call.receive<JoinGroupRequest>()
            val userId = call.getUserId()

            when (val result = groupService.joinGroup(request, userId)) {
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

        // Get group details
        get("/{id}") {
            val groupId = call.parameters["id"]?.toIntOrNull()
                ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    ResponseFactory.error(
                        error = "ValidationError",
                        message = "Invalid group ID"
                    )
                )

            val userId = call.getUserId()

            when (val result = groupService.getGroupById(groupId, userId)) {
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

        // Get members of a group
        get("/{id}/members") {
            val groupId = call.parameters["id"]?.toIntOrNull()
                ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    ResponseFactory.error(
                        error = "ValidationError",
                        message = "Invalid group ID"
                    )
                )

            val userId = call.getUserId()

            when (val result = groupService.getGroupMembers(groupId, userId)) {
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

        // Leave a group
        delete("/{id}/leave") {
            val groupId = call.parameters["id"]?.toIntOrNull()
                ?: return@delete call.respond(
                    HttpStatusCode.BadRequest,
                    ResponseFactory.error(
                        error = "ValidationError",
                        message = "Invalid group ID"
                    )
                )

            val userId = call.getUserId()

            when (val result = groupService.leaveGroup(groupId, userId)) {
                is Result.Success -> {
                    call.respond(
                        HttpStatusCode.OK,
                        ResponseFactory.success("Successfully left the group")
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