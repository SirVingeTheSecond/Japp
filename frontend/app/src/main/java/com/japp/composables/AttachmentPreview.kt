package com.japp.composables

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.japp.api.RetrofitClient
import com.japp.api.responses.attachment.AttachmentDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AttachmentPreview(
    attachment: AttachmentDto,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var thumbnailBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf(false) }

    LaunchedEffect(attachment.id) {
        coroutineScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.attachmentService.getThumbnail(attachment.id)
                }

                if (response.isSuccessful && response.body() != null) {
                    val bytes = withContext(Dispatchers.IO) {
                        response.body()!!.bytes()
                    }
                    thumbnailBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    isLoading = false
                } else {
                    loadError = true
                    isLoading = false
                }
            } catch (e: Exception) {
                loadError = true
                isLoading = false
            }
        }
    }

    Card(
        modifier = modifier
            .size(80.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
                loadError -> {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = "Failed to load",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(32.dp)
                    )
                }
                thumbnailBitmap != null -> {
                    Image(
                        bitmap = thumbnailBitmap!!.asImageBitmap(),
                        contentDescription = attachment.fileName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                else -> {
                    Icon(
                        Icons.Default.AttachFile,
                        contentDescription = "Attachment",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AttachmentThumbnailGrid(
    attachments: List<AttachmentDto>,
    onAttachmentClick: (AttachmentDto) -> Unit,
    onImageAttachmentClick: ((AttachmentDto) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (attachments.isEmpty()) return

    Column(modifier = modifier) {
        Text(
            "Attachments (${attachments.size})",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(4.dp))

        // Grid of thumbnails (max 4 per row)
        val rows = attachments.chunked(4)
        rows.forEach { rowAttachments ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(vertical = 2.dp)
            ) {
                rowAttachments.forEach { attachment ->
                    AttachmentPreview(
                        attachment = attachment,
                        onClick = {
                            val isImage = attachment.mimeType.startsWith("image/")
                            if (isImage && onImageAttachmentClick != null) {
                                onImageAttachmentClick(attachment)
                            } else {
                                onAttachmentClick(attachment)
                            }
                        }
                    )
                }
            }
        }
    }
}