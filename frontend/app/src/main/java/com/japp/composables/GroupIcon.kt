package com.japp.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.japp.ui.theme.JappTheme

@Composable
fun GroupIcon(
    content: String,
    modifier: Modifier = Modifier
) {
    // Extract initials from the group name
    var label = ""
    var preChar = ""
    for (c in content.toCharArray()) {
        if (preChar == "" || preChar == " ") {
            label += c
        }
        preChar = c.toString()
    }

    val containerColor = MaterialTheme.colorScheme.primaryContainer
    val contentColor = MaterialTheme.colorScheme.onPrimaryContainer

    Box(
        modifier = modifier
            .size(56.dp)
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(containerColor)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            text = label.uppercase(),
            autoSize = TextAutoSize.StepBased(),
            color = { contentColor },
            maxLines = 1
        )
    }
}

@Preview
@Composable
fun GroupIconPreview() {
    JappTheme {
        GroupIcon("Kotlin Trashers")
    }
}

@Preview
@Composable
fun GroupIconSingleWordPreview() {
    JappTheme {
        GroupIcon("Roommates")
    }
}
