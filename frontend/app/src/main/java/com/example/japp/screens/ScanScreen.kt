package com.example.japp.screens

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.example.japp.AppDestinations

@Composable
fun ScanScreen(navController: NavController) {
    Button(onClick = { navController.navigate(AppDestinations.HOME.label) }) {
        Text("HELLO (Go to home+????)")
    }
}