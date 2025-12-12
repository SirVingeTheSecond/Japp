package com.japp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavDeepLink
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.japp.api.CredentialsStorage
import com.japp.api.SessionManager
import com.japp.composables.OfflineBanner
import com.japp.messaging.JappMessagingService
import com.japp.screens.ActivityScreen
import com.japp.screens.CreateExpenseScreen
import com.japp.screens.CreateGroupScreen
import com.japp.screens.EditProfileScreen
import com.japp.screens.GROUP_ID
import com.japp.screens.GroupScreen
import com.japp.screens.HomeScreen
import com.japp.screens.JoinGroupScreen
import com.japp.screens.OptionsScreen
import com.japp.screens.ProfileScreen
import com.japp.screens.ScanScreen
import com.japp.screens.SettleGroup
import com.japp.screens.ShowGroupsScreen
import com.japp.ui.JappSnackbar
import com.japp.ui.LocalSnackbarHost
import com.japp.ui.NotificationPermissionHandler
import com.japp.ui.theme.JappTheme
import com.japp.utils.LocalConnectivity
import com.japp.utils.rememberConnectivityState
import com.japp.websocket.ChatWebSocketClient

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Ensure clean WebSocket state
        ChatWebSocketClient.disconnect()

        val credentials = CredentialsStorage.load(this)
        credentials?.let {
            ChatWebSocketClient.connect(it.accessToken)
        }

        setContent {
            JappTheme {
                JappApp()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ChatWebSocketClient.disconnect()
    }
}


// Fab stuff
data class FabState(
    val icon: ImageVector? = null,
    val onClick: (() -> Unit)? = null,
    val visible: Boolean = true
)

object FabController {
    var state by mutableStateOf(FabState())
}

object GroupNavController {
    var navController: NavController? = null
}

@Composable
fun rememberFabButton(
    icon: ImageVector? = null,
    visible: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    DisposableEffect(icon, visible, onClick) {
        FabController.state = FabState(
            icon = icon,
            onClick = onClick,
            visible = visible
        )
        onDispose {
            FabController.state = FabState()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JappApp() {
    val context = LocalContext.current
    val navController = rememberNavController()
    var currentDestination by rememberSaveable { mutableStateOf<AppDestinations?>(AppDestinations.HOME) }
    var notificationPermissionRequested by rememberSaveable { mutableStateOf(false) }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Connectivity state which is the single source of truth for entire app
    // Time constraints forced a pretty pragmatic solution ¯\_(ツ)_/¯
    val isConnected by rememberConnectivityState()

    // Snackbar state for feedback messages
    val snackbarHostState = remember { SnackbarHostState() }

    // Session expiration handling - all cleanup happens here
    val sessionExpired by SessionManager.sessionExpired.collectAsState()

    LaunchedEffect(sessionExpired) {
        if (sessionExpired) {
            // ALL THE CLEANUP HAPPENS HERE
            ChatWebSocketClient.disconnect()
            CredentialsStorage.clear(context)
            SessionManager.resetSessionState()

            val intent = Intent(context, StartupActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            context.startActivity(intent)
            (context as? Activity)?.finish()
        }
    }

    if (!notificationPermissionRequested) {
        NotificationPermissionHandler { granted ->
            if (granted) {
                JappMessagingService.refreshToken(context)
            }
        }
    }

    navController.addOnDestinationChangedListener { _, destination, _ ->
        currentDestination = AppDestinations.entries.find { it.route == destination.route }
    }

    fun navigate(route: String) {
        if (currentDestination?.route != route) {
            navController.navigate(route)
        }
    }

    // Provide connectivity and snackbar state to all child composables
    CompositionLocalProvider(
        LocalConnectivity provides isConnected,
        LocalSnackbarHost provides snackbarHostState
    ) {
        Scaffold(
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState) { data ->
                    JappSnackbar(snackbarData = data)
                }
            },
            topBar = {
                // Not the cleanest approach...
                // Determine if we are on a screen that needs a back button
                val isOnOptionsScreen = currentRoute?.startsWith("group/") == true
                        && currentRoute.endsWith("/options")

                val title = when {
                    isOnOptionsScreen -> "Group Options"
                    else -> currentDestination?.label ?: ""
                }

                CenterAlignedTopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    title = {
                        Text(title)
                    },
                    navigationIcon = {
                        if (isOnOptionsScreen) {
                            // Back button for Options screen
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                        } else {
                            // Activity icon with badge for main screens
                            IconButton(onClick = { navigate(AppDestinations.ACTIVITY.route) }) {
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
                        }
                    },
                    actions = {
                        // Only show actions when not on Options screen
                        if (!isOnOptionsScreen) {
                            if (currentRoute == AppDestinations.GROUP.route) {
                                // Navigate to GROUP_OPTIONS as a separate screen
                                IconButton(onClick = {
                                    navController.navigate(
                                        AppDestinations.CustomRoutes.GROUP_OPTIONS.withArgs(GROUP_ID)
                                    )
                                }) {
                                    Icon(
                                        imageVector = Icons.Filled.Settings,
                                        contentDescription = "Options"
                                    )
                                }
                            } else {
                                IconButton(onClick = { navigate(AppDestinations.CREATEGROUP.route) }) {
                                    Icon(
                                        imageVector = AppDestinations.CREATEGROUP.icon,
                                        contentDescription = "Create Group"
                                    )
                                }
                            }
                        }
                    },
                )
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = AppDestinations.HOME.route == currentDestination?.route,
                        onClick = { navigate(AppDestinations.HOME.route) },
                        icon = {
                            Icon(
                                imageVector = if (AppDestinations.HOME.route == currentDestination?.route)
                                    Icons.Filled.Home
                                else
                                    Icons.Outlined.Home,
                                contentDescription = "Navigate to Home",
                                modifier = Modifier.size(32.dp)
                            )
                        },
                        label = {
                            Text(AppDestinations.HOME.label)
                        },
                        alwaysShowLabel = true,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    val fab = FabController.state
                    val actionButton = NavigationActionButtons.entries
                        .find { it.route == currentDestination?.route }
                        ?: NavigationActionButtons.DEFAULT
                    FloatingActionButton(
                        onClick = {
                            fab.onClick?.invoke() ?: navigate(actionButton.destination.route)
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    ) {
                        Icon(fab.icon ?: actionButton.icon, null)
                    }

                    NavigationBarItem(
                        selected = AppDestinations.PROFILE.route == currentDestination?.route,
                        onClick = { navigate(AppDestinations.PROFILE.route) },
                        icon = {
                            Icon(
                                imageVector = if (AppDestinations.PROFILE.route == currentDestination?.route)
                                    Icons.Filled.Person
                                else
                                    Icons.Outlined.Person,
                                contentDescription = "Navigate to Profile",
                                modifier = Modifier.size(32.dp)
                            )
                        },
                        label = {
                            Text(AppDestinations.PROFILE.label)
                        },
                        alwaysShowLabel = true,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            },
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.surface
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // shows when disconnected
                OfflineBanner(isOffline = !isConnected)

                // Main content
                NavHost(
                    navController = navController,
                    startDestination = AppDestinations.HOME.route,
                    modifier = Modifier.weight(1f)
                ) {
                    for (destination in AppDestinations.entries) {
                        composable(destination.route) { destination.screen(navController) }
                    }

                    for (destination in AppDestinations.CustomRoutes.entries) {
                        composable(
                            route = destination.route,
                            arguments = destination.arguments,
                            deepLinks = destination.deepLinks
                        ) {
                            destination.screen(navController, it)
                        }
                    }
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
    SCAN("Scan", Icons.Default.Camera, { navController -> ScanScreen(navController) }),
    PROFILE("Profile", Icons.Default.Person, { navController -> ProfileScreen(navController) }),
    CREATEGROUP("Create Group", Icons.Default.GroupAdd, { navController -> CreateGroupScreen(navController) }),
    MYGROUPS("My Groups", Icons.Default.Groups, { navController -> ShowGroupsScreen(navController) }),
    GROUP("Group", Icons.Default.Group, { navController -> GroupScreen(navController) }),

    ACTIVITY("Activity", Icons.Default.Notifications, { navController -> ActivityScreen(navController) }),
    EDITPROFILE("EditProfile", Icons.Default.ManageAccounts, { navController -> EditProfileScreen(navController) });

    val route: String
        get() = label.replace(" ", "")

    enum class CustomRoutes(
        val label: String,
        val route: String,
        val buildRoute: ((List<Any>) -> String)? = null,
        val arguments: List<NamedNavArgument> = emptyList(),
        val deepLinks: List<NavDeepLink> = emptyList(),
        val screen: @Composable (NavController, NavBackStackEntry) -> Unit
    ) {
        JOINGROUP(
            "Join Group",
            route = "join/{code}",
            arguments = listOf(navArgument("code") { type = NavType.StringType }),
            deepLinks = listOf(navDeepLink {
                uriPattern = "japp://join/{code}"
            }),
            screen = { navController, backStackEntry ->
                val code = backStackEntry.arguments?.getString("code")
                JoinGroupScreen(navController, code)
            }
        ),
        CREATE_EXPENSE(
            "Add Expense",
            route = "expense/create/{groupId}",
            buildRoute = { args ->
                "expense/create/${args[0]}"
            },
            arguments = listOf(navArgument("groupId") { type = NavType.IntType }),
            screen = { navController, backStackEntry ->
                CreateExpenseScreen(navController, backStackEntry.arguments?.getInt("groupId"))
            }
        ),
        SETTLE_GROUP(
            "Settle Group",
            route = "settle/{groupId}",
            buildRoute = { args ->
                "settle/${args[0]}"
            },
            arguments = listOf(navArgument("groupId") { type = NavType.IntType }),
            screen = { navController, backStackEntry ->
                SettleGroup(navController, backStackEntry.arguments?.getInt("groupId"))
            }
        ),
        GROUP_OPTIONS(
            "Group Options",
            route = "group/{groupId}/options",
            buildRoute = { args ->
                "group/${args[0]}/options"
            },
            arguments = listOf(navArgument("groupId") { type = NavType.IntType }),
            screen = { navController, backStackEntry ->
                OptionsScreen(navController, backStackEntry.arguments?.getInt("groupId") ?: -1)
            }
        );

        fun withArgs(vararg args: Any): String {
            return buildRoute?.invoke(args.toList()) ?: route
        }
    }
}

enum class NavigationActionButtons(
    val route: String,
    val buttonIcon: ImageVector?,
    val destination: AppDestinations,
) {
    DEFAULT("", null, AppDestinations.SCAN),
    // TODO: Make actual conditional buttons.
    GROUPADD(AppDestinations.GROUP.route, Icons.Default.Add, AppDestinations.GROUP);

    val icon: ImageVector
        get() = buttonIcon ?: destination.icon
}
