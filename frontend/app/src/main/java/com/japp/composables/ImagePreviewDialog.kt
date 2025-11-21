package com.japp.composables

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.japp.api.RetrofitClient
import com.japp.api.responses.attachment.AttachmentDto
import com.japp.utils.AttachmentDownloadHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ImagePreviewDialog(
    attachment: AttachmentDto,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var imageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf(false) }
    var isOpeningExternal by remember { mutableStateOf(false) }

    LaunchedEffect(attachment.id) {
        try {
            val response = withContext(Dispatchers.IO) {
                RetrofitClient.attachmentService.downloadAttachment(attachment.id)
            }

            if (response.isSuccessful && response.body() != null) {
                val bytes = withContext(Dispatchers.IO) {
                    response.body()!!.bytes()
                }
                imageBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
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

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(500.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                when {
                    isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Loading image...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    loadError -> {
                        Text(
                            "Failed to load image",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            attachment.fileName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    imageBitmap != null -> {
                        Spacer(Modifier.height(16.dp))
                        Image(
                            bitmap = imageBitmap!!.asImageBitmap(),
                            contentDescription = attachment.fileName,
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                                .background(Color.Transparent),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            attachment.fileName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    if (!isLoading && !loadError) {
                        Button(
                            onClick = {
                                isOpeningExternal = true
                                coroutineScope.launch {
                                    val result = AttachmentDownloadHelper.downloadAndOpen(
                                        context = context,
                                        attachmentId = attachment.id,
                                        fileName = attachment.fileName,
                                        mimeType = attachment.mimeType
                                    )
                                    isOpeningExternal = false

                                    if (result.isSuccess) {
                                        onDismiss()
                                    } else {
                                        android.widget.Toast.makeText(
                                            context,
                                            "Failed to open image",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            },
                            enabled = !isOpeningExternal,
                            modifier = Modifier.padding(8.dp),
                        ) {
                            if (isOpeningExternal) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(Modifier.width(8.dp))
                            }
                            Text("View Full Size")
                        }
                    }

                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.padding(8.dp),
                    ) {
                        Text("Dismiss")
                    }
                }
            }
        }
    }
}