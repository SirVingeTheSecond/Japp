package com.japp.screens


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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.japp.api.RetrofitClient
import com.japp.api.responses.activity.ActivityDto
import com.japp.composables.ActivityRow
import retrofit2.Callback
import retrofit2.Response


fun getActivities(
    userActivity: MutableState<List<ActivityDto>?>,
    isLoading: MutableState<Boolean>,
    limit: Int? = null
) {
    val call = RetrofitClient.activityService.get_user_activities(limit = limit)

    call.enqueue(object : Callback<List<ActivityDto>?> {

        override fun onResponse(
            call: retrofit2.Call<List<ActivityDto>?>,
            response: Response<List<ActivityDto>?>
        ) {
            isLoading.value = false

            val body = response.body()

            if (body != null && response.isSuccessful) {
                userActivity.value = body
            }

        }

        override fun onFailure(
            call: retrofit2.Call<List<ActivityDto>?>,
            t: Throwable
        ) {
            isLoading.value = false
            TODO("Not yet implemented")
        }
    })
}

@Preview(showSystemUi = true)
@Composable
fun ActivityScreen(navController: NavController? = null) {

    val userActivity = remember {  mutableStateOf<List<ActivityDto>?>(null) }
    val isLoading = remember { mutableStateOf<Boolean>(true) }

    LaunchedEffect(Unit) {
        getActivities(userActivity, isLoading)
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