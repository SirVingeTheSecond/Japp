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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
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
import com.japp.api.responses.activity.ActivityDto
import com.japp.composables.ActivityRow
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.Date
import kotlin.collections.forEach
import kotlin.math.round

@Preview(showSystemUi = true)
@Composable
fun HomeScreen(navController: NavController? = null) {
    Column(
        Modifier.fillMaxSize().padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        QuickStats()
        HorizontalDivider(
            Modifier.padding(10.dp).background(MaterialTheme.colorScheme.primary),
            thickness = 2.dp
        )
        QuickActivities(navController)
        HorizontalDivider(
            Modifier.padding(10.dp).background(MaterialTheme.colorScheme.primary),
            thickness = 2.dp
        )
        QuickGroups(navController)
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
fun QuickActivities(navController: NavController?) {

    val userActivity = remember {  mutableStateOf<List<ActivityDto>?>(null) }
    val isLoading = remember { mutableStateOf<Boolean>(true) }

    LaunchedEffect(Unit) {
        getActivities(userActivity, isLoading, 3)
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

@Composable
fun QuickGroups(navController: NavController?) {
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
    }
}