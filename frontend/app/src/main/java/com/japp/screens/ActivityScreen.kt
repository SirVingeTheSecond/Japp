package com.japp.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavController

@Composable
fun ActivityScreen(navController: NavController) {
    Column() {
        Text("Basic research is what I am doing when I don’t know what I am doing.")
        Text("I don't know what that is...")
        Text("But it was an activity.")
    }
}