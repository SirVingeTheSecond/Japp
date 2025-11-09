package com.example.japp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.japp.AppDestinations
import com.example.japp.ui.theme.Dimens

@Composable
fun ProfileScreen(navController: NavController) {
    Scaffold(modifier = Modifier
        .background(MaterialTheme.colorScheme.primary))
    { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // Profile icon (This should be default when users haven't set their own one)
            // TODO: Fetch image from backend
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile icon",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // TODO: Fetch display values from backend
            Text(
                text = "John Doe",
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                text = "john.doe@example.com",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            Text(
                text = "+45 12 34 56 78",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )

            Spacer(modifier = Modifier.height(Dimens.spacingMedium))
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimens.spacingSmall),
                onClick = { "TODO: Implement" }
            ) {
                Text("Edit profile")
            }

            Spacer(modifier = Modifier.height(Dimens.spacingSmall))
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimens.spacingSmall),
                onClick = { "TODO: Implement" }
            ) {
                Text("Settings")
            }


            Spacer(modifier = Modifier.height(Dimens.spacingLarge))

            OutlinedButton(
                onClick = { navController.navigate(AppDestinations.HOME.route) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimens.spacingSmall),
            ) {
                Text("Log out")
            }
        }
    }
}
