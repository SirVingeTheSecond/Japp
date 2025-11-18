package com.japp.composables


import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.japp.ui.theme.JappTheme

@Composable
fun GroupIcon(content: String, modifier: Modifier = Modifier){
    var label = ""
    var preChar = ""
    for (c in content.toCharArray()){
        if (preChar == "" || preChar == " "){
            label += c
        }
        preChar = c.toString()
    }

    val color = MaterialTheme.colorScheme.onPrimaryContainer

    Box(
        modifier = modifier.then(Modifier
            .size(150.dp)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(150.dp))
            .border(2.dp, Color.Gray, RoundedCornerShape(150.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(15.dp),
        ),


        contentAlignment = Alignment.Center
    ) {
        BasicText(
            label.uppercase(),
            autoSize = TextAutoSize.StepBased(),
            color = { color },
            maxLines = 1
        )

    }
}

@Preview()
@Composable
fun GroupIconPreview(){
    JappTheme {
        GroupIcon("Kotlin Trashers")
    }
}