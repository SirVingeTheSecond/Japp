package com.japp.screens


import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.japp.api.ErrorUtils
import com.japp.api.RetrofitClient
import com.japp.api.responses.activity.ActivityDto
import com.japp.composables.ActivityRow


suspend fun getActivities(
    userActivity: MutableState<List<ActivityDto>?>,
    isLoading: MutableState<Boolean>,
    context: Context,
    limit: Int? = null
) {
    val res = RetrofitClient.activityService.getUserActivities(limit = limit)
    if (res.isSuccessful && res.body() != null) {
        userActivity.value = res.body()
        isLoading.value = false
    } else {
        ErrorUtils.handleError(res, context)
    }
}

@Preview(showSystemUi = true)
@Composable
fun ActivityScreen(navController: NavController? = null) {
    val context = LocalContext.current

    val userActivity = remember {  mutableStateOf<List<ActivityDto>?>(null) }
    val isLoading = remember { mutableStateOf<Boolean>(true) }

    LaunchedEffect(Unit) {
        getActivities(userActivity, isLoading, context)
    }

    Column (
    modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
    verticalArrangement = Arrangement.Top,
    horizontalAlignment = Alignment.CenterHorizontally
    ) {

        if (isLoading.value) {
            CircularProgressIndicator()
        } else {

            userActivity.value?.forEach {
                ActivityRow(
                    it
                )
            }

        }

    }
}