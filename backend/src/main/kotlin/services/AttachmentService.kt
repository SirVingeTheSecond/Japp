package com.japp.services

import com.japp.models.Result
import com.japp.models.dto.AttachmentDto
import com.japp.models.dto.AttachmentListDto
import com.japp.models.error.AttachmentError
import com.japp.repositories.interfaces.IAttachmentRepository
import com.japp.repositories.interfaces.IExpenseRepository
import com.japp.repositories.interfaces.IGroupRepository
import com.japp.repositories.interfaces.IUserRepository
import com.japp.utils.toDto
import com.japp.validation.AttachmentValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.io.File
import java.io.InputStream
import java.util.UUID

// Yeah, integer IDs will not be changed to UUIDs because I'm a lazy mf'er
class AttachmentService(
    private val attachmentRepository: IAttachmentRepository,
    private val expenseRepository: IExpenseRepository,
    private val groupRepository: IGroupRepository,
    private val userRepository: IUserRepository,
    private val activityService: ActivityService,
    private val storageBasePath: String
) {

    suspend fun uploadAttachment(
        expenseId: Int,
        userId: Int,
        fileName: String,
        fileSize: Long,
        mimeType: String,
        inputStream: InputStream
    ): Result<AttachmentDto, AttachmentError> {
        return when (val validation = AttachmentValidator.validateFileUpload(fileName, fileSize, mimeType)) {
            is Result.Failure -> validation
            is Result.Success -> {
                withContext(Dispatchers.IO) {
                    try {
                        val result = transaction {
                            val expense = expenseRepository.findById(expenseId)
                                ?: return@transaction Result.Failure(
                                    AttachmentError.ExpenseNotFound(expenseId)
                                )

                            if (!groupRepository.isMember(expense.groupId, userId)) {
                                return@transaction Result.Failure(
                                    AttachmentError.NotMember(expense.groupId)
                                )
                            }

                            // Unique file path
                            val extension = File(fileName).extension
                            val uniqueFileName = "${UUID.randomUUID()}.$extension"
                            val relativePath = "$expenseId/$uniqueFileName"
                            val fullPath = File(storageBasePath, relativePath)

                            // This only creates the directory if it does not exist.
                            fullPath.parentFile.mkdirs()

                            fullPath.outputStream().use { output ->
                                inputStream.copyTo(output)
                            }

                            val attachment = attachmentRepository.create(
                                expenseId = expenseId,
                                uploadedBy = userId,
                                fileName = fileName,
                                storagePath = relativePath,
                                fileSize = fileSize,
                                mimeType = mimeType
                            )

                            val uploader = userRepository.findById(userId)
                            val downloadUrl = "/api/attachments/${attachment.id}/download"

                            Result.Success(
                                Pair(
                                    attachment.toDto(
                                        uploaderName = uploader?.username ?: "Unknown",
                                        downloadUrl = downloadUrl
                                    ),
                                    expense.groupId
                                )
                            )
                        }

                        when (result) {
                            is Result.Success -> {
                                val (attachmentDto, groupId) = result.value

                                activityService.logReceiptUploaded(
                                    groupId = groupId,
                                    userId = userId,
                                    expenseId = expenseId,
                                    expenseDescription = "" // We'll get this separately if needed
                                )

                                Result.Success(attachmentDto)
                            }
                            is Result.Failure -> result
                        }
                    } catch (e: Exception) {
                        Result.Failure(
                            AttachmentError.InternalError(e.message ?: "Failed to upload attachment")
                        )
                    }
                }
            }
        }
    }

    suspend fun getExpenseAttachments(
        expenseId: Int,
        userId: Int
    ): Result<AttachmentListDto, AttachmentError> {
        return withContext(Dispatchers.IO) {
            try {
                transaction {
                    val expense = expenseRepository.findById(expenseId)
                        ?: return@transaction Result.Failure(
                            AttachmentError.ExpenseNotFound(expenseId)
                        )

                    if (!groupRepository.isMember(expense.groupId, userId)) {
                        return@transaction Result.Failure(
                            AttachmentError.NotMember(expense.groupId)
                        )
                    }

                    val attachments = attachmentRepository.findByExpenseId(expenseId)

                    val attachmentDtos = attachments.map { attachment ->
                        val uploader = userRepository.findById(attachment.uploadedBy)
                        val downloadUrl = "/api/attachments/${attachment.id}/download"

                        attachment.toDto(
                            uploaderName = uploader?.username ?: "Unknown",
                            downloadUrl = downloadUrl
                        )
                    }

                    Result.Success(
                        AttachmentListDto(
                            expenseId = expenseId,
                            attachments = attachmentDtos
                        )
                    )
                }
            } catch (e: Exception) {
                Result.Failure(
                    AttachmentError.InternalError(e.message ?: "Failed to retrieve attachments")
                )
            }
        }
    }

    // It is definitely not the smartest approach to hardcode this

    suspend fun deleteAttachment(
        attachmentId: Int,
        userId: Int
    ): Result<Unit, AttachmentError> {
        return withContext(Dispatchers.IO) {
            try {
                transaction {
                    val attachment = attachmentRepository.findById(attachmentId)
                        ?: return@transaction Result.Failure(
                            AttachmentError.NotFound(attachmentId)
                        )

                    if (attachment.uploadedBy != userId) {
                        return@transaction Result.Failure(
                            AttachmentError.Unauthorized("Only the uploader can delete this attachment")
                        )
                    }

                    val expense = expenseRepository.findById(attachment.expenseId)
                        ?: return@transaction Result.Failure(
                            AttachmentError.ExpenseNotFound(attachment.expenseId)
                        )

                    if (!groupRepository.isMember(expense.groupId, userId)) {
                        return@transaction Result.Failure(
                            AttachmentError.NotMember(expense.groupId)
                        )
                    }

                    // Delete DB record
                    attachmentRepository.delete(attachmentId)

                    // Delete the file
                    val file = File(storageBasePath, attachment.storagePath)
                    if (file.exists()) {
                        file.delete()
                    }

                    Result.Success(Unit)
                }
            } catch (e: Exception) {
                Result.Failure(
                    AttachmentError.InternalError(e.message ?: "Failed to delete attachment")
                )
            }
        }
    }
}
