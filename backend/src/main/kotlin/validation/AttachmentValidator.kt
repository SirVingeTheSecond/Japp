package com.japp.validation

import com.japp.models.Result
import com.japp.models.error.AttachmentError
import java.io.File

object AttachmentValidator {

    fun validateFileUpload(
        fileName: String,
        fileSize: Long,
        mimeType: String
    ): Result<Unit, AttachmentError> {
        // Validate file size
        if (fileSize <= 0) {
            return Result.Failure(
                AttachmentError.ValidationError("File is empty")
            )
        }

        if (fileSize > ValidationConstants.Attachment.MAX_FILE_SIZE) {
            val maxSizeMB = ValidationConstants.Attachment.MAX_FILE_SIZE / (1024 * 1024)
            return Result.Failure(
                AttachmentError.ValidationError("File size exceeds maximum of ${maxSizeMB}MB")
            )
        }

        // Validate type
        if (mimeType !in ValidationConstants.Attachment.ALLOWED_MIME_TYPES) {
            return Result.Failure(
                AttachmentError.ValidationError(
                    "Invalid file type. Only PNG and JPEG images are allowed"
                )
            )
        }

        // Validate file extension
        val extension = File(fileName).extension.lowercase()
        if (extension !in ValidationConstants.Attachment.ALLOWED_EXTENSIONS) {
            return Result.Failure(
                AttachmentError.ValidationError(
                    "Invalid file extension. Only .png, .jpg, .jpeg are allowed"
                )
            )
        }

        return Result.Success(Unit)
    }
}