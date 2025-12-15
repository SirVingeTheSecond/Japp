package com.japp.routes

import com.japp.models.Result
import com.japp.models.dto.FcmTokenRequest
import com.japp.models.dto.UpdateUserRequest
import com.japp.plugins.getUserId
import com.japp.services.UserService
import com.japp.utils.ResponseFactory
import com.japp.utils.requirePathInt
import com.japp.utils.respondResult
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.readRemaining
import kotlinx.io.readByteArray
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

        post("/me/pp") {
            val userId = call.getUserId()

            var fileName: String? = null
            var fileSize: Long? = null
            var mimeType: String? = null
            var fileBytes: ByteArray? = null

            try {
                val multipart = call.receiveMultipart()

                multipart.forEachPart { part ->
                    when (part) {
                        is PartData.FileItem -> {
                            fileName = part.originalFileName ?: "profile.jpg"
                            mimeType = part.contentType?.toString() ?: "image/jpeg"
                            fileBytes = part.provider().readRemaining().readByteArray()
                            fileSize = fileBytes!!.size.toLong()
                        }
                        else -> {}
                    }
                    part.dispose()
                }

                if (fileName == null || fileSize == null || mimeType == null || fileBytes == null) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ResponseFactory.error(
                            error = "ValidationError",
                            message = "Image file is required"
                        )
                    )
                    return@post
                }

                val result = userService.uploadProfilePicture(
                    userId = userId,
                    fileName = fileName!!,
                    fileSize = fileSize!!,
                    mimeType = mimeType!!,
                    inputStream = fileBytes!!.inputStream()
                )

                call.respondResult(result)

            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ResponseFactory.error(
                        error = "InternalServerError",
                        message = e.message ?: "Failed to upload profile picture"
                    )
                )
            }
        }

        delete("/me/pp") {
            val userId = call.getUserId()
            val result = userService.deleteProfilePicture(userId)
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

        get("/{id}") {
            val targetUserId = call.requirePathInt("id")
            val result = userService.getUserProfile(targetUserId)
            call.respondResult(result)
        }

        get("/{id}/pp") {
            val targetUserId = call.requirePathInt("id")

            when (val result = userService.getProfilePicture(targetUserId)) {
                is Result.Success -> {
                    val file = result.value
                    val contentType = when (file.extension.lowercase()) {
                        "jpg", "jpeg" -> ContentType.Image.JPEG
                        "png" -> ContentType.Image.PNG
                        else -> ContentType.Application.OctetStream
                    }
                    call.response.header(
                        HttpHeaders.ContentType,
                        contentType.toString()
                    )
                    call.response.header(
                        HttpHeaders.CacheControl,
                        "max-age=3600"
                    )
                    call.respondFile(file)
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
