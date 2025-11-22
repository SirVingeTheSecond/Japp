package com.japp.routes

import com.japp.models.Result
import com.japp.plugins.getUserId
import com.japp.services.AttachmentService
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

fun Route.attachmentRoutes() {
    val attachmentService by inject<AttachmentService>()

    route("/attachments") {

        // Upload attachment to expense
        post {
            val userId = call.getUserId()

            var expenseId: Int? = null
            var fileName: String? = null
            var fileSize: Long? = null
            var mimeType: String? = null
            var fileBytes: ByteArray? = null

            try {
                val multipart = call.receiveMultipart()

                multipart.forEachPart { part ->
                    when (part) {
                        is PartData.FormItem -> {
                            if (part.name == "expenseId") {
                                expenseId = part.value.toIntOrNull()
                            }
                        }
                        is PartData.FileItem -> {
                            fileName = part.originalFileName ?: "unknown"
                            mimeType = part.contentType?.toString() ?: "application/octet-stream"
                            fileBytes = part.provider().readRemaining().readByteArray()
                            fileSize = fileBytes.size.toLong()
                        }
                        else -> {}
                    }
                    part.dispose()
                }

                if (expenseId == null) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ResponseFactory.error(
                            error = "ValidationError",
                            message = "expenseId is required"
                        )
                    )
                    return@post
                }

                if (fileName == null || fileSize == null || mimeType == null || fileBytes == null) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ResponseFactory.error(
                            error = "ValidationError",
                            message = "File is required"
                        )
                    )
                    return@post
                }

                val result = attachmentService.uploadAttachment(
                    expenseId = expenseId!!,
                    userId = userId,
                    fileName = fileName,
                    fileSize = fileSize,
                    mimeType = mimeType,
                    inputStream = fileBytes.inputStream()
                )

                call.respondResult(result, HttpStatusCode.Created)

            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ResponseFactory.error(
                        error = "InternalServerError",
                        message = e.message ?: "Failed to upload attachment"
                    )
                )
            }
        }

        // Download attachment file
        get("/{id}/download") {
            val attachmentId = call.requirePathInt("id")
            val userId = call.getUserId()

            when (val result = attachmentService.getAttachment(attachmentId, userId)) {
                is Result.Success -> {
                    val (file) = result.value
                    call.response.header(
                        HttpHeaders.ContentDisposition,
                        ContentDisposition.Attachment.withParameter(
                            ContentDisposition.Parameters.FileName,
                            file.name
                        ).toString()
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

        // Get attachment thumbnail
        get("/{id}/thumbnail") {
            val attachmentId = call.requirePathInt("id")
            val userId = call.getUserId()

            when (val result = attachmentService.getAttachmentThumbnail(attachmentId, userId)) {
                is Result.Success -> {
                    val thumbnailFile = result.value
                    call.response.header(
                        HttpHeaders.ContentType,
                        ContentType.Image.JPEG.toString()
                    )
                    call.response.header(
                        HttpHeaders.CacheControl,
                        "max-age=86400" // 24 hours
                    )
                    call.respondFile(thumbnailFile)
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

        // List all attachments for an expense
        get("/expense/{expenseId}") {
            val expenseId = call.requirePathInt("expenseId")
            val userId = call.getUserId()

            val result = attachmentService.getAttachments(expenseId, userId)
            call.respondResult(result)
        }

        // Delete attachment
        delete("/{id}") {
            val attachmentId = call.requirePathInt("id")
            val userId = call.getUserId()

            val result = attachmentService.deleteAttachment(attachmentId, userId)
            call.respondResult(result)
        }
    }
}