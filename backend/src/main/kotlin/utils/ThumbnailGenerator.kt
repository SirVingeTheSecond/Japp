package com.japp.utils

import java.awt.Image
import java.awt.image.BufferedImage
import java.io.File
import java.io.FileOutputStream
import javax.imageio.ImageIO

object ThumbnailGenerator {

    private const val THUMBNAIL_SIZE = 200
    //private const val THUMBNAIL_QUALITY = 0.85f

    /**
     * Generates a thumbnail for an image file.
     * Returns the thumbnail file if successful, null otherwise.
     *
     * Thumbnails are cached as such:
     * - Original: /storage/attachments/expense_123/image.jpg
     * - Thumbnail: /storage/thumbnails/expense_123/image.jpg
     */
    fun generateThumbnail(
        originalFile: File,
        storageBasePath: String
    ): File? {
        return try {
            // Only process image files
            if (!isImageFile(originalFile)) {
                return null
            }

            // Read original image
            val originalImage = ImageIO.read(originalFile) ?: return null

            // Calculate thumbnail dimensions maintaining aspect ratio
            val (thumbnailWidth, thumbnailHeight) = calculateThumbnailDimensions(
                originalImage.width,
                originalImage.height,
                THUMBNAIL_SIZE
            )

            // Create thumbnail
            val scaledImage = originalImage.getScaledInstance(
                thumbnailWidth,
                thumbnailHeight,
                Image.SCALE_SMOOTH
            )

            val thumbnail = BufferedImage(
                thumbnailWidth,
                thumbnailHeight,
                BufferedImage.TYPE_INT_RGB
            )

            val graphics = thumbnail.createGraphics()
            graphics.drawImage(scaledImage, 0, 0, null)
            graphics.dispose()

            // Determine thumbnail path
            val thumbnailFile = getThumbnailPath(originalFile, storageBasePath)
            thumbnailFile.parentFile.mkdirs()

            // Save thumbnail
            FileOutputStream(thumbnailFile).use { output ->
                ImageIO.write(thumbnail, "jpg", output)
            }

            thumbnailFile
        } catch (e: Exception) {
            // Log error but don't fail - thumbnails are optional
            null
        }
    }

    /**
     * Gets the cached thumbnail file for an original file.
     * Returns null if thumbnail does not exist.
     */
    fun getThumbnailFile(
        originalFile: File,
        storageBasePath: String
    ): File? {
        val thumbnailFile = getThumbnailPath(originalFile, storageBasePath)
        return if (thumbnailFile.exists()) thumbnailFile else null
    }

    /**
     * Calculates thumbnail dimensions and maintains aspect ratio.
     * The longest side equals maxSize.
     */
    private fun calculateThumbnailDimensions(
        originalWidth: Int,
        originalHeight: Int,
        maxSize: Int
    ): Pair<Int, Int> {
        val aspectRatio = originalWidth.toDouble() / originalHeight.toDouble()

        return if (originalWidth > originalHeight) {
            // Landscape
            val width = maxSize
            val height = (maxSize / aspectRatio).toInt()
            width to height
        } else {
            // Portrait or square
            val width = (maxSize * aspectRatio).toInt()
            val height = maxSize
            width to height
        }
    }

    /**
     * Determines the file path for the thumbnail based on original file path.
     */
    private fun getThumbnailPath(
        originalFile: File,
        storageBasePath: String
    ): File {
        val relativePath = originalFile.absolutePath.removePrefix(storageBasePath)
        val thumbnailPath = relativePath.replace("/attachments/", "/thumbnails/")
        return File(storageBasePath, thumbnailPath)
    }

    /**
     * Checks if file is an image based on extension.
     */
    private fun isImageFile(file: File): Boolean {
        val imageExtensions = setOf("jpg", "jpeg", "png")
        val extension = file.extension.lowercase()
        return extension in imageExtensions
    }

    /**
     * Deletes thumbnail when original attachment is deleted.
     */
    fun deleteThumbnail(
        originalFile: File,
        storageBasePath: String
    ) {
        try {
            val thumbnailFile = getThumbnailPath(originalFile, storageBasePath)
            if (thumbnailFile.exists()) {
                thumbnailFile.delete()
            }
        } catch (_: Exception) {
        }
    }
}