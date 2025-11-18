package com.japp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.japp.api.RetrofitClient
import com.japp.api.responses.activity.ActivityDto
import com.japp.api.responses.activity.GroupActivitiesDto
import com.japp.composables.printableDatetime
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import retrofit2.Callback
import retrofit2.Response
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

@Composable
fun ActivityRow(
    icon: ImageVector,
    user: String,
    action: String,
    date: LocalDateTime
) {
    return Row (
            modifier = Modifier
                .padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "Activity type",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(24.dp)
            )

            Text(
                text = user,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = action,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = printableDatetime(date),
                style = MaterialTheme.typography.labelSmall
            )
        }
}


fun getActivities(
    userActivity: MutableState<List<ActivityDto>?>,
    isLoading: MutableState<Boolean>
) {
    val call = RetrofitClient.activityService.get_user_activities()

    call.enqueue(object: Callback<List<ActivityDto>?> {

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
                    Icons.Default.Circle, // todo, icon per actionType
                    it.userName,
                    it.actionType.description,
                    LocalDateTime.ofInstant(Instant.ofEpochMilli(it.createdAt.toLong()), ZoneId.systemDefault())
                )
            }

        }

    }
}