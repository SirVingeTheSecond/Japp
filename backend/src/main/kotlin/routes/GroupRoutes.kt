package com.japp.routes

import com.japp.models.dto.AddMemberRequest
import com.japp.models.dto.CreateGroupRequest
import com.japp.models.dto.JoinGroupRequest
import com.japp.plugins.getUserId
import com.japp.services.GroupService
import com.japp.utils.requirePathInt
import com.japp.utils.respondResult
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.groupRoutes() {
    val groupService by inject<GroupService>()

    route("/groups") {

        post {
            val request = call.receive<CreateGroupRequest>()
            val userId = call.getUserId()
            val result = groupService.createGroup(request, userId)
            call.respondResult(result, HttpStatusCode.Created)
        }

        get {
            val userId = call.getUserId()
            val result = groupService.getUserGroups(userId)
            call.respondResult(result)
        }

        post("/join") {
            val request = call.receive<JoinGroupRequest>()
            val userId = call.getUserId()
            val result = groupService.joinGroup(request, userId)
            call.respondResult(result)
        }

        get("/{id}") {
            val groupId = call.requirePathInt("id")
            val userId = call.getUserId()
            val result = groupService.getGroupById(groupId, userId)
            call.respondResult(result)
        }

        get("/{id}/members") {
            val groupId = call.requirePathInt("id")
            val userId = call.getUserId()
            val result = groupService.getGroupMembers(groupId, userId)
            call.respondResult(result)
        }

        post("/{id}/members") {
            val groupId = call.requirePathInt("id")
            val request = call.receive<AddMemberRequest>()
            val userId = call.getUserId()
            val result = groupService.addMember(groupId, request.userId, userId)
            call.respondResult(result, HttpStatusCode.Created)
        }

        delete("/{id}/leave") {
            val groupId = call.requirePathInt("id")
            val userId = call.getUserId()
            val result = groupService.leaveGroup(groupId, userId)
            call.respondResult(result)
        }

        get("/{id}/invite") {
            val groupId = call.requirePathInt("id")
            val userId = call.getUserId()
            val result = groupService.getGroupInviteDetails(groupId, userId)
            call.respondResult(result)
        }
    }
}