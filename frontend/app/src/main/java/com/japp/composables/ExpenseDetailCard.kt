package com.japp.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.japp.api.RetrofitClient
import com.japp.api.responses.attachment.AttachmentDto
import com.japp.api.responses.expense.ExpenseDto
import com.japp.utils.AttachmentDownloadHelper
import com.japp.utils.DateTimeHelper
import kotlinx.coroutines.launch

@Composable
fun ExpenseDetailCard(
    expense: ExpenseDto,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var attachments by remember { mutableStateOf<List<AttachmentDto>>(emptyList()) }
    var isLoadingAttachments by remember { mutableStateOf(true) }
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(expense.id) {
        try {
            val res = RetrofitClient.attachmentService.getExpenseAttachments(expense.id)
            if (res.isSuccessful && res.body() != null) {
                attachments = res.body()!!.attachments
            }
        } catch (_: Exception) {
            // No attachments to show
        } finally {
            isLoadingAttachments = false
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { expanded = !expanded }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        expense.description,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        DateTimeHelper.formatRelative(expense.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    "${expense.amount} ${expense.currency.symbol}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(4.dp))

            Text(
                "Paid by: ${expense.paidByName}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                "Split: ${expense.splitType}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!isLoadingAttachments && attachments.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AttachFile,
                        contentDescription = "Has attachments",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "${attachments.size} attachment${if (attachments.size != 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            if (expanded) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))

                expense.category?.let {
                    Text(
                        "Category: ${it.displayName}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(4.dp))
                }

                Text(
                    "Created: ${DateTimeHelper.formatDateTime(expense.createdAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (expense.splits.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Split details:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    expense.splits.forEach { split ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                split.username,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                split.shareAmount?.let { "${it} ${expense.currency.symbol}" }
                                    ?: split.sharePercentage?.let { "${it}%" }
                                    ?: "Equal",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                if (attachments.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))

                    var selectedImageAttachment by remember { mutableStateOf<AttachmentDto?>(null) }

                    AttachmentThumbnailGrid(
                        attachments = attachments,
                        onAttachmentClick = { attachment ->
                            coroutineScope.launch {
                                val result = AttachmentDownloadHelper.downloadAndOpen(
                                    context = context,
                                    attachmentId = attachment.id,
                                    fileName = attachment.fileName,
                                    mimeType = attachment.mimeType
                                )

                                if (result.isFailure) {
                                    android.widget.Toast.makeText(
                                        context,
                                        "Failed to download: ${attachment.fileName}",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    android.widget.Toast.makeText(
                                        context,
                                        "Opening ${attachment.fileName}",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        },
                        onImageAttachmentClick = { attachment ->
                            selectedImageAttachment = attachment
                        }
                    )

                    selectedImageAttachment?.let { attachment ->
                        ImagePreviewDialog(
                            attachment = attachment,
                            onDismiss = { selectedImageAttachment = null }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AttachmentItem(
    attachment: AttachmentDto,
    isDownloading: Boolean = false,
    onDownload: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    attachment.fileName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "Uploaded by ${attachment.uploaderName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    AttachmentDownloadHelper.formatFileSize(attachment.fileSize),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isDownloading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                IconButton(onClick = onDownload) {
                    Icon(
                        Icons.Default.Download,
                        contentDescription = "Download attachment",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
