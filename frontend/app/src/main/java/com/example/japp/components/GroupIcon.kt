package com.example.japp.components


import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.japp.ui.theme.JappTheme

@Composable
fun GroupIcon(content: String){
    var label = ""
    var preChar = ""
    for (c in content.toCharArray()){
        if (preChar == "" || preChar == " "){
            label += c
        }
        preChar = c.toString()
    }

    val color = MaterialTheme.colorScheme.tertiary

    Box(
        modifier = Modifier
            .size(150.dp)
            .clip(RoundedCornerShape(150.dp))
            .border(2.dp, Color.Gray, RoundedCornerShape(150.dp))
            .background(Color(0xFFEFEFEF))
            .padding(15.dp),


        contentAlignment = Alignment.Center
    ) {
        BasicText(
            label,
            autoSize = TextAutoSize.StepBased(),
            color = { color }
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