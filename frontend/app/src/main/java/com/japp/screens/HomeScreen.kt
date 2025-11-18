package com.japp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import androidx.navigation.NavController
import com.japp.AppDestinations
import com.japp.api.RetrofitClient
import com.japp.api.responses.activity.ActivityDto
import com.japp.api.responses.activity.GroupActivitiesDto
import com.japp.api.responses.group.GroupDto
import com.japp.composables.TimeText
import kotlinx.coroutines.delay
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Date
import kotlin.math.round
import kotlin.random.Random

@Preview(showSystemUi = true)
@Composable
fun HomeScreen(navController: NavController? = null) {
    var groups by remember { mutableStateOf<List<GroupDto>?>(null) }
    var recentActivities by remember { mutableStateOf<List<ActivityDto>?>(null) }

    LaunchedEffect(Unit) {
        val call = RetrofitClient.groupService.get_my_groups()
        call.enqueue(object: Callback<List<GroupDto>?>{
            override fun onResponse(
                call: Call<List<GroupDto>?>,
                response: Response<List<GroupDto>?>
            ) {
                val body = response.body()
                if (body != null && response.isSuccessful) {
                    if (body.isEmpty()) {
                        groups = body
                        return
                    }
                    groups = body.sortedBy {
                        Random.nextBoolean()
                    }.slice(IntRange(0, 2.coerceAtMost(body.size-1)))
                }
            }

            override fun onFailure(
                call: Call<List<GroupDto>?>,
                t: Throwable
            ) {
                TODO("Not yet implemented")
            }

        })
    }
    LaunchedEffect(groups) {
        if (groups == null) return@LaunchedEffect
        for (group in groups) {
            val call = RetrofitClient.activityService.get_group_activities(group.id)
            call!!.enqueue(object: Callback<GroupActivitiesDto?>{
                override fun onResponse(
                    call: Call<GroupActivitiesDto?>,
                    response: Response<GroupActivitiesDto?>
                ) {
                    val body = response.body()
                    if (body != null && response.isSuccessful) {
                        if (recentActivities == null) { recentActivities = arrayListOf() }
                        recentActivities = recentActivities?.plus(body.activities)
                    }
                }

                override fun onFailure(
                    call: Call<GroupActivitiesDto?>,
                    t: Throwable
                ) {
                    TODO("Not yet implemented")
                }

            })
        }
        recentActivities = recentActivities?.sortedBy { dto -> dto.createdAt.toLong() }
        if (recentActivities == null) { recentActivities = arrayListOf() }
    }

    Column(
        Modifier.fillMaxSize().padding(10.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        QuickStats()
        HorizontalDivider(
            Modifier.padding(10.dp).background(MaterialTheme.colorScheme.primary),
            thickness = 2.dp
        )
        QuickActivities(navController, recentActivities)
        HorizontalDivider(
            Modifier.padding(10.dp).background(MaterialTheme.colorScheme.primary),
            thickness = 2.dp
        )
        QuickGroups(navController, groups)
    }
}


@Composable
fun QuickStats() {
    var owed by remember { mutableStateOf<Int?>(null) }
    var ratio by remember { mutableStateOf<Float?>(null) }
    var owes by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) {
        // TODO: Get expenses
        delay(2000)
        owed = 300
        owes = 50
        ratio = round((owes!!.toFloat() / (owed!! + owes!!).toFloat()) * 100) / 100
    }

    var acceptColor = Color(0xFF20DF6C)
    var errorColor = Color(0xFFDF2020)
    var ratioColorInt = ratio?.let { ColorUtils.blendARGB(acceptColor.toArgb(), errorColor.toArgb(), it) }
    var ratioColor = Color(ratioColorInt ?: Color.Yellow.toArgb())

    Row (
        Modifier.fillMaxWidth().height(80.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        if (owed != null && ratio != null && owes != null) {
            Pill(owed.toString(), label = "Owed", color = acceptColor, textColor = MaterialTheme.colorScheme.onPrimaryContainer)
            Pill((ratio!!*100f).toInt().toString() + "%", label = "Ratio", color = ratioColor, textColor = MaterialTheme.colorScheme.onTertiaryContainer)
            Pill(owes.toString(), label = "Owes", color = errorColor, textColor = MaterialTheme.colorScheme.onSecondaryContainer)
        } else {
            LinearProgressIndicator(
                Modifier.align(Alignment.CenterVertically),
                color = MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }
    }
}

@Composable
fun Pill(content: String = "Idk?", label: String? = null, color: Color? = null, textColor: Color? = null) {
    Column (
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        label?.let { Text(it) }
        Box (
            Modifier.clip(RoundedCornerShape(100.dp)).background(color ?: MaterialTheme.colorScheme.primaryContainer).padding(vertical = 6.dp, horizontal = 12.dp)
        ) {
            Text(content, color = textColor ?: MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
fun QuickActivities(navController: NavController?, activities: List<ActivityDto>?) {

    Column(
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Recent activities", style = MaterialTheme.typography.headlineSmall)
            TextButton(
                onClick = { navController?.navigate(AppDestinations.ACTIVITY.route) }
            ) {
                Text("Activities ->", textAlign = TextAlign.End)
            }
        }
        if (activities != null) {
            for (activity in activities) {
                Activity(activity)
            }
        } else {
            LinearProgressIndicator(
                Modifier.align(Alignment.CenterHorizontally),
                color = MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }
    }
}

@Composable
fun Activity(activity: ActivityDto) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            Modifier.fillMaxWidth(0.7f),
        ) {
            Text(activity.userName)
            Text(activity.description)
            Text(activity.actionType.toString())
        }
        TimeText(Date(activity.createdAt.toLong()))
    }
}

@Composable
fun QuickGroups(navController: NavController?, groups: List<GroupDto>? = null) {

    Column(
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Groups", style = MaterialTheme.typography.headlineSmall)
            TextButton(
                onClick = { navController?.navigate(AppDestinations.MYGROUPS.route) }
            ) {
                Text("My Groups ->", textAlign = TextAlign.End)
            }
        }
        //TODO: Use group card
        if (groups != null) {
            groups.forEach { dto ->
                GroupCard(dto, onClick = {
                    GROUP_ID = dto.id
                    navController?.navigate(AppDestinations.GROUP.route)
                })
            }
        } else {
            LinearProgressIndicator(
                Modifier.align(Alignment.CenterHorizontally),
                color = MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }
    }
}