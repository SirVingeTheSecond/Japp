package com.japp.screens

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.japp.api.NetworkResult
import com.japp.api.RetrofitClient
import com.japp.api.responses.Currency
import com.japp.api.responses.ExpenseCategory
import com.japp.api.responses.SplitType
import com.japp.api.responses.expense.CreateExpenseRequest
import com.japp.api.responses.expense.ExpenseSplitRequest
import com.japp.api.responses.group.GroupDto
import com.japp.api.responses.group.GroupMemberDto
import com.japp.api.safeApiCall
import com.japp.ui.rememberSnackbar
import com.japp.ui.state.UiState
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

enum class SplitInputMode {
    AMOUNT,
    PERCENTAGE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateExpenseScreen(
    navController: NavController? = null,
    groupId: Int? = null,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbar = rememberSnackbar()

    var groupsState by remember { mutableStateOf<UiState<List<GroupDto>>>(UiState.Loading) }
    var selectedGroup by remember { mutableStateOf<GroupDto?>(null) }

    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<ExpenseCategory?>(null) }

    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var splitType by remember { mutableStateOf(SplitType.EQUAL) }
    var splitInputMode by remember { mutableStateOf(SplitInputMode.AMOUNT) }

    var groupMembers by remember { mutableStateOf<List<GroupMemberDto>>(emptyList()) }
    var memberShares by remember { mutableStateOf<Map<Int, String>>(emptyMap()) }

    var selectedImageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var uploadingAttachments by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableStateOf("") }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            selectedImageUris = selectedImageUris + it
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let {
            // Save bitmap to temp file and get URI
            val file = File(context.cacheDir, "camera_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
            }
            selectedImageUris = selectedImageUris + Uri.fromFile(file)
        }
    }

    LaunchedEffect(Unit) {
        groupsState = when (val result = safeApiCall("CreateExpense.groups") {
            RetrofitClient.groupService.getMyGroups()
        }) {
            is NetworkResult.Success -> {
                val groups = result.data
                if (groupId != null) {
                    selectedGroup = groups.find { it.id == groupId }
                }
                UiState.Success(groups)
            }

            is NetworkResult.Error -> UiState.Error(result.message)
        }
    }

    LaunchedEffect(selectedGroup?.id) {
        val group = selectedGroup ?: return@LaunchedEffect
        safeApiCall("CreateExpense.members") {
            RetrofitClient.groupService.getGroupMembers(group.id)
        }.onSuccess {
            groupMembers = it
            memberShares = groupMembers.associate { member -> member.userId to "" }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.Start,
    ) {
        Text("Create Expense", style = MaterialTheme.typography.titleLarge)

        Spacer(Modifier.height(16.dp))

        when (groupsState) {
            is UiState.Loading -> {
                Text("Loading groups...")
            }

            is UiState.Error -> {
                Text(
                    text = (groupsState as UiState.Error).message,
                    color = MaterialTheme.colorScheme.error
                )
            }

            is UiState.Success -> {
                val groups = (groupsState as UiState.Success<List<GroupDto>>).data
                if (groups.isEmpty()) {
                    Text("You are not in any groups")
                } else {
                    var expanded by remember { mutableStateOf(false) }

                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = selectedGroup?.name ?: "Select group",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Group") },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )

                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            groups.forEach { group ->
                                DropdownMenuItem(
                                    text = { Text(group.name) },
                                    onClick = {
                                        selectedGroup = group
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Text("Split type", style = MaterialTheme.typography.titleMedium)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = splitType == SplitType.EQUAL,
                            onClick = { splitType = SplitType.EQUAL }
                        )
                        Text("Equal")
                    }

                    Spacer(Modifier.width(16.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = splitType == SplitType.CUSTOM,
                            onClick = { splitType = SplitType.CUSTOM }
                        )
                        Text("Custom")
                    }
                }

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        // Check if the new input contains any wordlike in it.
                        if (!it.contains("\\w")) {
                            // Only if it doesn't contain letters, set it.
                            // Ensure it can be converted to a double.
                            val newVal = it.toDoubleOrNull()
                            if (newVal != null) {
                                amount = it
                            }
                        }
                    },
                    label = { Text("Amount") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 1,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                Spacer(Modifier.height(8.dp))

                // Nullable boolean so that it's possible to know when description has been checked.
                // Preventing the box from starting off being invalid.
                var descriptionValid by remember { mutableStateOf<Boolean?>(null) }

                OutlinedTextField(
                    value = description,
                    onValueChange = {
                        description = it
                        if (description.isEmpty()) {
                            descriptionValid = false
                        }
                    },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = !(descriptionValid ?: true),
                    supportingText = {
                        descriptionValid?.let {
                            if (!it) {
                                Text("Description cannot be left empty")
                            }
                        }
                    }
                )

                Spacer(Modifier.height(8.dp))

                var categoryExpanded by remember { mutableStateOf(false) }

                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded }
                ) {
                    OutlinedTextField(
                        value = category?.displayName ?: "No category",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category (optional)") },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        DropdownMenuItem(
                            { Text("No category") },
                            onClick = {
                                category = null
                                categoryExpanded = false
                            }
                        )
                        ExpenseCategory.entries.forEach { _category ->
                            DropdownMenuItem(
                                text = { Text(_category.displayName) },
                                onClick = {
                                    category = _category
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                if (splitType == SplitType.CUSTOM) {
                    if (groupMembers.isEmpty()) {
                        Text("No members in group")
                    } else {
                        Text("Custom split mode", style = MaterialTheme.typography.titleMedium)

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = splitInputMode == SplitInputMode.AMOUNT,
                                    onClick = { splitInputMode = SplitInputMode.AMOUNT }
                                )
                                Text("Amount")
                            }

                            Spacer(Modifier.width(16.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = splitInputMode == SplitInputMode.PERCENTAGE,
                                    onClick = { splitInputMode = SplitInputMode.PERCENTAGE }
                                )
                                Text("Percentage")
                            }
                        }

                        groupMembers.forEach { member ->
                            val currentValue = memberShares[member.userId] ?: ""
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Text(
                                    member.username,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = currentValue,
                                    onValueChange = { newValue ->
                                        memberShares = memberShares + (member.userId to newValue)
                                    },
                                    label = {
                                        Text(
                                            when (splitInputMode) {
                                                SplitInputMode.AMOUNT -> "Amount"
                                                SplitInputMode.PERCENTAGE -> "Percent"
                                            }
                                        )
                                    },
                                    modifier = Modifier.width(120.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Attachments section

                HorizontalDivider()

                Spacer(Modifier.height(8.dp))

                Text("Attachments (optional)", style = MaterialTheme.typography.titleMedium)

                Spacer(Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            imagePickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        enabled = !isSubmitting && !uploadingAttachments
                    ) {
                        Icon(Icons.Default.AttachFile, contentDescription = "Add from gallery")
                        Spacer(Modifier.width(4.dp))
                        Text("Gallery")
                    }

                    OutlinedButton(
                        onClick = { cameraLauncher.launch(null) },
                        enabled = !isSubmitting && !uploadingAttachments
                    ) {
                        Icon(Icons.Default.AttachFile, contentDescription = "Take photo")
                        Spacer(Modifier.width(4.dp))
                        Text("Camera")
                    }
                }

                // Display selected images
                if (selectedImageUris.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))

                    selectedImageUris.forEach { uri ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Load and display
                                val bitmap = remember(uri) {
                                    try {
                                        context.contentResolver.openInputStream(uri)
                                            ?.use { stream ->
                                                BitmapFactory.decodeStream(stream)
                                            }
                                    } catch (_: Exception) {
                                        null
                                    }
                                }

                                if (bitmap != null) {
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = "Selected image",
                                        modifier = Modifier.size(60.dp)
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.Image,
                                        contentDescription = "Image",
                                        modifier = Modifier.size(60.dp)
                                    )
                                }

                                Spacer(Modifier.width(8.dp))

                                Text(
                                    uri.lastPathSegment ?: "Image",
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodySmall
                                )

                                IconButton(
                                    onClick = {
                                        selectedImageUris = selectedImageUris.filter { it != uri }
                                    },
                                    enabled = !isSubmitting && !uploadingAttachments
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove")
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                if (isSubmitting) {
                    CircularProgressIndicator()
                } else {
                    Button(
                        onClick = {
                            if (description.isEmpty()) {
                                descriptionValid = false
                                return@Button
                            }

                            val group = selectedGroup
                            if (group == null) {
                                errorMessage = "Please select a group"
                                return@Button
                            }

                            val amountValue = amount.toDoubleOrNull()
                            if (amountValue == null || amountValue <= 0) {
                                errorMessage = "Invalid amount"
                                return@Button
                            }

                            if (description.isBlank()) {
                                errorMessage = "Description is required"
                                return@Button
                            }

                            val splitsForRequest =
                                if (splitType == SplitType.CUSTOM) {
                                    groupMembers.mapNotNull { member ->
                                        val raw = memberShares[member.userId].orEmpty().trim()
                                        if (raw.isEmpty()) return@mapNotNull null

                                        val value = raw.toDoubleOrNull() ?: return@mapNotNull null

                                        when (splitInputMode) {
                                            SplitInputMode.AMOUNT -> ExpenseSplitRequest(
                                                userId = member.userId,
                                                shareAmount = value,
                                                sharePercentage = null
                                            )

                                            SplitInputMode.PERCENTAGE -> ExpenseSplitRequest(
                                                userId = member.userId,
                                                shareAmount = null,
                                                sharePercentage = value
                                            )
                                        }
                                    }
                                } else {
                                    null
                                }

                            val request = CreateExpenseRequest(
                                groupId = group.id,
                                amount = amountValue,
                                description = description,
                                category = category,
                                currency = Currency.DKK,
                                splitType = splitType,
                                splits = splitsForRequest
                            )

                            isSubmitting = true
                            coroutineScope.launch {
                                when (val result = safeApiCall("CreateExpense.create") {
                                    RetrofitClient.expenseService.createExpense(request)
                                }) {
                                    is NetworkResult.Success -> {
                                        val createdExpense = result.data

                                        // Upload attachments if any selected
                                        if (selectedImageUris.isNotEmpty()) {
                                            uploadingAttachments = true
                                            uploadProgress = "Uploading attachments..."

                                            var successCount = 0
                                            selectedImageUris.forEachIndexed { index, uri ->
                                                uploadProgress =
                                                    "Uploading ${index + 1}/${selectedImageUris.size}..."

                                                try {
                                                    // Copy URI content to temp file for upload
                                                    val inputStream =
                                                        context.contentResolver.openInputStream(uri)
                                                    val file = File(
                                                        context.cacheDir,
                                                        "upload_${System.currentTimeMillis()}.jpg"
                                                    )

                                                    inputStream?.use { input ->
                                                        FileOutputStream(file).use { output ->
                                                            input.copyTo(output)
                                                        }
                                                    }

                                                    // Create request
                                                    // Perhaps not the cleanest approach
                                                    val mimeType =
                                                        when (file.extension.lowercase()) {
                                                            "png" -> "image/png"
                                                            "jpg", "jpeg" -> "image/jpeg"
                                                            else -> "image/jpeg"
                                                        }
                                                    val requestBody =
                                                        file.asRequestBody(mimeType.toMediaTypeOrNull())
                                                    val filePart =
                                                        MultipartBody.Part.createFormData(
                                                            "file",
                                                            file.name,
                                                            requestBody
                                                        )
                                                    val expenseIdBody = createdExpense.id.toString()
                                                        .toRequestBody("text/plain".toMediaTypeOrNull())

                                                    val uploadRes =
                                                        RetrofitClient.attachmentService.uploadAttachment(
                                                            expenseId = expenseIdBody,
                                                            file = filePart
                                                        )

                                                    if (uploadRes.isSuccessful) {
                                                        successCount++
                                                    }

                                                    // Clean up temp file
                                                    file.delete()
                                                } catch (_: Exception) {
                                                    // Continue with other uploads even if one fails
                                                }
                                            }

                                            uploadingAttachments = false

                                            val message = if (successCount == selectedImageUris.size) {
                                                "Expense created with all attachments!"
                                            } else {
                                                "Expense created ($successCount/${selectedImageUris.size} attachments uploaded)"
                                            }
                                            snackbar.showSuccess(message)
                                        } else {
                                            snackbar.showSuccess("Expense created!")
                                        }

                                        navController?.navigateUp()
                                    }

                                    is NetworkResult.Error -> {
                                        snackbar.showError(result.message, onRetry = {
                                            // Re-trigger submit - the button click handler will be called
                                        })
                                        errorMessage = result.message
                                        isSubmitting = false
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSubmitting && !uploadingAttachments
                    ) {
                        if (isSubmitting) {
                            Text("Creating...")
                        } else if (uploadingAttachments) {
                            Text("Uploading...")
                        } else {
                            Text("Create Expense")
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    if (uploadProgress.isNotEmpty()) {
                        Text(uploadProgress, color = MaterialTheme.colorScheme.primary)
                    }

                    errorMessage?.let {
                        Text(it, color = Color.Red)
                    }
                }
            }
        }
    }
}
