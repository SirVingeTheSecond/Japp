package com.example.japp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.japp.screens.ActivityScreen
import com.example.japp.screens.CreateGroupScreen
import com.example.japp.screens.GroupScreen
import com.example.japp.screens.HomeScreen
import com.example.japp.screens.ProfileScreen
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
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }
    navController.addOnDestinationChangedListener(
        listener = { controller, destination, arguments ->
            // This nullpointr shouldn't fail 🙏
            currentDestination = AppDestinations.entries.find { it.route == destination.route }!!
        }
    )

    fun navigate(route: String) {
        if (currentDestination.route != route) {
            navController.navigate(route)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onSecondary
                ),
                title = {
                    Text(currentDestination.label)
                },
                navigationIcon = {
                    IconButton(onClick = { navigate(AppDestinations.ACTIVITY.route) }) {
                        // Lil red dot on top left icon
                        BadgedBox(
                            badge = {
                                Badge()
                            }
                        ) {
                            Icon(
                                imageVector = AppDestinations.ACTIVITY.icon,
                                contentDescription = "Activities",
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { navigate(AppDestinations.CREATEGROUP.route) }) {
                        Icon(
                            imageVector = AppDestinations.CREATEGROUP.icon,
                            contentDescription = "Create Group"
                        )
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = AppDestinations.HOME.route == currentDestination.route,
                    onClick = { navigate(AppDestinations.HOME.route) },
                    icon = { Icon(AppDestinations.HOME.icon, "idk")},
                    label = {
                        Text(AppDestinations.HOME.label)
                    },
                    alwaysShowLabel = false,
                )
                val actionButton = NavigationActionButtons.entries.find { it.route == currentDestination.route } ?: NavigationActionButtons.DEFAULT
                FloatingActionButton (
                    onClick = { navigate(actionButton.destination.route) },
                    // Tendency to hit home or profile instead of scan, but moves them far apart. TODO: Better solution
                    Modifier.padding(24.dp, 0.dp)
                ) {
                    Icon(actionButton.icon, actionButton.destination.route)
                }
                NavigationBarItem(
                    selected = AppDestinations.PROFILE.route == currentDestination.route,
                    onClick = { navigate(AppDestinations.PROFILE.route) },
                    icon = { Icon(AppDestinations.PROFILE.icon, "idk")},
                    label = {
                        Text(AppDestinations.PROFILE.label)
                    },
                    alwaysShowLabel = false,
                )
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        NavHost(
            navController=navController,
            startDestination = AppDestinations.HOME.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            for (destination in AppDestinations.entries) {
                composable(destination.route) { destination.screen(navController) }
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
    SCAN("Scan", Icons.Default.Camera, { navController -> ScanScreen(navController) }),
    PROFILE("Profile", Icons.Default.Person, { navController -> ProfileScreen(navController) }),
    ACTIVITY("Activity", Icons.Default.Notifications, { navController -> ActivityScreen(navController) }),
    CREATEGROUP("Create Group", Icons.Default.GroupAdd, { navController -> CreateGroupScreen(navController) }),

    GROUP("Group", Icons.Default.Group, { navController -> GroupScreen(navController) });

    val route: String
        get() = label.replace(" ", "") // Remove spaces for route
}

enum class NavigationActionButtons(
    val route: String, // Route to match
    val buttonIcon: ImageVector?,
    val destination: AppDestinations
) {
    DEFAULT("", null, AppDestinations.SCAN),
    // TODO: Make actual conditional buttons.
    GROUPADD("Group", Icons.Default.Add, AppDestinations.ACTIVITY);

    val icon: ImageVector
        get() = buttonIcon ?: destination.icon // If buttonIcon is null defaults to AppDestination icon
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