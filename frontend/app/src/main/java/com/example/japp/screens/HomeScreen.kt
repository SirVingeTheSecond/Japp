package com.example.japp.screens

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.example.japp.AppDestinations

@Composable
fun HomeScreen(navController: NavController) {
    Button(onClick = { navController.navigate(AppDestinations.SCAN.label) }) {
        Text("HELLO (Go to qr?)")
    }
}