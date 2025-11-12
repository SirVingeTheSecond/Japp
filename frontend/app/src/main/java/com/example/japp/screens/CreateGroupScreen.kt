package com.example.japp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.modifier.modifierLocalOf
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.japp.AppDestinations
import com.example.japp.components.GroupIcon

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun CreateGroupScreen(navController: NavController? = null) {
    var name by remember { mutableStateOf("") }
    var descript by remember { mutableStateOf("") }
    Box(
        modifier = Modifier.fillMaxSize(),
        Alignment.Center
    ){
        Column (
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ){
            GroupIcon(name)
            OutlinedTextField(
                name,
                onValueChange = {
                    name = it
                },
                label = { Text("Group name") }
            )
            OutlinedTextField(
                descript,
                onValueChange = {descript = it},
                label = { Text("Description") }
            )
            Button(onClick = {}) {
                Text("Submit")
            }

        }

    }
}

