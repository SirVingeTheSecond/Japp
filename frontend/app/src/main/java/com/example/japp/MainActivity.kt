package com.example.japp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.japp.screens.HomeScreen
import com.example.japp.screens.ScanScreen
import com.example.japp.ui.theme.JappTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JappTheme {
                JappApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@PreviewScreenSizes
@Composable
fun JappApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.subList(0, 3).forEach {
                item(
                    icon = {
                        Icon(
                            it.icon,
                            contentDescription = it.label
                        )
                    },
                    label = { Text(it.label) },
                    selected = currentRoute == it.label,
                    onClick = {
                        currentDestination = it
                        if (currentRoute != it.label) {
                            navController.navigate(it.label) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(currentDestination.label) },
                    colors = TopAppBarColors(
                        containerColor = Color.Blue,
                        scrolledContainerColor = Color.Transparent,
                        navigationIconContentColor = Color.Black,
                        titleContentColor = Color.Red,
                        actionIconContentColor = Color.Black
                    ),
                    navigationIcon = {
                        IconButton(onClick = { navController.navigate(AppDestinations.ACTIVITY.label) }) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = "Localized description"
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { navController.navigate(AppDestinations.CREATEGROUP.label) }) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = "Localized description"
                            )
                        }
                    },
                )
            },
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            NavHost(
                navController=navController,
                startDestination = AppDestinations.HOME.label,
                modifier = Modifier.padding(innerPadding)
            ) {
                for (destination in AppDestinations.entries) {
                    composable(destination.label) { destination.screen(navController) }
                }
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
    val screen: @Composable (NavController) -> Unit
) {
    HOME("Home", Icons.Default.Home, { navController -> HomeScreen(navController) }),
    SCAN("Scan", Icons.Default.Search, { navController -> ScanScreen(navController) }),
    PROFILE("Profile", Icons.Default.AccountBox, { navController -> HomeScreen(navController) }),
    ACTIVITY("Activity", Icons.Default.List, { navController -> ScanScreen(navController) }),
    CREATEGROUP("CreateGroup", Icons.Default.Create, { navController -> HomeScreen(navController) })
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    JappTheme {
        Greeting("Android")
    }
}