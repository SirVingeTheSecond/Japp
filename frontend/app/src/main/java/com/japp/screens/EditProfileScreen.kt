package com.japp.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.japp.api.NetworkResult
import com.japp.api.RetrofitClient
import com.japp.api.responses.auth.UserDto
import com.japp.api.responses.user.UpdateUserRequest
import com.japp.api.safeApiMutation
import com.japp.api.safeApiQuery
import com.japp.ui.rememberSnackbar
import com.japp.ui.state.UiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = rememberSnackbar()

    var userState by remember { mutableStateOf<UiState<UserDto>>(UiState.Loading) }

    var firstname by remember { mutableStateOf("") }
    var lastname by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var currentProfilePictureUrl by remember { mutableStateOf<String?>(null) }
    var currentUserId by remember { mutableIntStateOf(0) }

    var saving by remember { mutableStateOf(false) }
    var uploadingImage by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            uploadProfilePicture(
                context = context,
                uri = it,
                onStart = { uploadingImage = true },
                onSuccess = { updatedUser ->
                    uploadingImage = false
                    currentProfilePictureUrl = updatedUser.profilePicture
                    selectedImageUri = null
                    snackbar.showSuccess("Profile picture updated!")
                },
                onError = { message ->
                    uploadingImage = false
                    selectedImageUri = null
                    snackbar.showError(message)
                },
                scope = scope
            )
        }
    }

    LaunchedEffect(Unit) {
        userState = when (val result = safeApiQuery("EditProfile.load") {
            RetrofitClient.userService.getMyUser()
        }) {
            is NetworkResult.Success -> {
                val user = result.data
                firstname = user.firstname
                lastname = user.lastname
                phone = user.phone.orEmpty()
                currentProfilePictureUrl = user.profilePicture
                currentUserId = user.id
                UiState.Success(user)
            }
            is NetworkResult.Error -> UiState.Error(result.message)
        }
    }

    fun save() {
        if (saving) return
        saving = true

        scope.launch {
            val request = UpdateUserRequest(
                firstname = firstname,
                lastname = lastname,
                phone = phone.ifBlank { null }
            )

            when (val result = safeApiMutation("EditProfile.save") {
                RetrofitClient.userService.updateMyUser(request)
            }) {
                is NetworkResult.Success -> {
                    snackbar.showSuccess("Profile updated!")
                    navController.popBackStack()
                }
                is NetworkResult.Error -> {
                    snackbar.showError(result.message, onRetry = { save() })
                    saving = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        when (userState) {
            is UiState.Loading -> {
                Box(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is UiState.Error -> {
                Box(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = (userState as UiState.Error).message,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            is UiState.Success -> {
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    ProfilePictureSelector(
                        currentUrl = currentProfilePictureUrl,
                        selectedUri = selectedImageUri,
                        isUploading = uploadingImage,
                        userId = currentUserId,
                        onClick = { imagePickerLauncher.launch("image/*") }
                    )

                    Spacer(Modifier.height(24.dp))

                    OutlinedTextField(
                        value = firstname,
                        onValueChange = { firstname = it },
                        label = { Text("First name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = lastname,
                        onValueChange = { lastname = it },
                        label = { Text("Last name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(Modifier.height(24.dp))

                    Button(
                        onClick = { save() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !saving && !uploadingImage
                    ) {
                        Text(if (saving) "Saving..." else "Save")
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfilePictureSelector(
    currentUrl: String?,
    selectedUri: Uri?,
    isUploading: Boolean,
    userId: Int,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .size(120.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .clickable(enabled = !isUploading) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        when {
            isUploading -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(40.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            selectedUri != null -> {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(selectedUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Selected profile picture",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            currentUrl != null -> {
                val imageUrl = "${RetrofitClient.BASE_URL}user/$userId/pp"
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Current profile picture",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            else -> {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (!isUploading) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Change photo",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

private fun uploadProfilePicture(
    context: android.content.Context,
    uri: Uri,
    onStart: () -> Unit,
    onSuccess: (UserDto) -> Unit,
    onError: (String) -> Unit,
    scope: CoroutineScope
) {
    onStart()

    scope.launch {
        try {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(uri) ?: "image/jpeg"

            val extension = when (mimeType) {
                "image/png" -> "png"
                else -> "jpg"
            }

            val tempFile = File(context.cacheDir, "profile_upload.$extension")
            contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            val requestBody = tempFile.asRequestBody(mimeType.toMediaTypeOrNull())
            val filePart = MultipartBody.Part.createFormData(
                "file",
                "profile.$extension",
                requestBody
            )

            when (val result = safeApiMutation("EditProfile.uploadPicture") {
                RetrofitClient.userService.uploadProfilePicture(filePart)
            }) {
                is NetworkResult.Success -> {
                    tempFile.delete()
                    onSuccess(result.data)
                }
                is NetworkResult.Error -> {
                    tempFile.delete()
                    onError(result.message)
                }
            }
        } catch (e: Exception) {
            onError(e.message ?: "Failed to upload image")
        }
    }
}
