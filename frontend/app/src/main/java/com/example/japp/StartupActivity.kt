package com.example.japp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Patterns;
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.japp.ui.theme.JappTheme

class StartupActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JappTheme {
                StartupPage(LocalContext.current)
            }
        }
    }
}

enum class Screens(
    val label: String,
) {
    LOGIN("Login"),
    SIGNUP("Sign Up");

    val route: String
        get() = label.replace(" ", "")
}

@Composable
fun StartupPage(context: Context) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screens.LOGIN.route
    ) {
        composable(Screens.LOGIN.route ) { LoginScreen(context, navController) }
        composable(Screens.SIGNUP.route ) { SignupScreen(context, navController) }
    }

//    LaunchedEffect(key1 = true) {
//        context.startActivity(Intent(context, MainActivity::class.java))
//    }
}

@Composable
fun LoginScreen(context: Context, navController: NavController) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    fun login() {
        if (username == "admin" && password == "admin") {
            val intent = Intent(context, MainActivity::class.java)
            intent.putExtra("username", username)
            context.startActivity(intent)
        } else {
            error = true
        }
    }

    Scaffold { innerPadding ->
        Column (
            Modifier.padding(innerPadding).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Login!", style = MaterialTheme.typography.displaySmall)
            Box(Modifier.clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceContainer).padding(10.dp)) {
                Column (
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Column (

                    ) {
                        OutlinedTextField(
                            username,
                            onValueChange = { newText -> username = newText},
                            singleLine = true,
                            label = { Text("Username") },
                            isError = error
                        )
                        OutlinedTextField(
                            password,
                            onValueChange = { newText -> password = newText},
                            singleLine = true,
                            label = { Text("Password") },
                            isError = error,
                            visualTransformation = PasswordVisualTransformation()
                        )
                    }
                    Button(onClick = { login() }) {
                        Text("Login!")
                    }
                    Column (
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Don't have an account?")
                        Text(
                            buildAnnotatedString {
                                withLink(
                                    LinkAnnotation.Clickable(
                                        "",
                                        TextLinkStyles(style = SpanStyle(color = Color.Blue)),
                                        linkInteractionListener = {
                                            navController.navigate(Screens.SIGNUP.route)
                                        }
                                    )
                                ) {
                                    append("Sign up!")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SignupScreen(context: Context, navController: NavController) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var repeatPassword by remember { mutableStateOf("") }

    var isUsernameValid by remember { mutableStateOf(true) }
    var isEmailValid by remember { mutableStateOf(true) }
    var isPasswordValid by remember { mutableStateOf(true) }
    var isRepeatPasswordValid by remember { mutableStateOf(true) }

    fun signup() {
        if (username == "admin" && password == "admin") {
            navController.navigate(Screens.LOGIN.route)
        } else {
            // Something went wrong
            // TODO: Show to user?
        }
    }

    Scaffold { innerPadding ->
        Column (
            Modifier.padding(innerPadding).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Signup!", style = MaterialTheme.typography.displaySmall)
            Box(Modifier.clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceContainer).padding(10.dp)) {
                Column (
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Column (

                    ) {
                        OutlinedTextField(
                            username,
                            onValueChange = {
                                username = it
                                isUsernameValid = (it.length >= 5)
                            },
                            singleLine = true,
                            label = { Text("Username") },
                            isError = !isUsernameValid,
                            supportingText = {
                                if (!isUsernameValid) {
                                    Text("Username must be at least 5 characters")
                                }
                            }
                        )
                        OutlinedTextField(
                            email,
                            onValueChange = {
                                email = it
                                isEmailValid = Patterns.EMAIL_ADDRESS.matcher(it).matches()
                            },
                            singleLine = true,
                            label = { Text("Email") },
                            isError = !isEmailValid,
                            supportingText = {
                                if (!isEmailValid) {
                                    Text("Email is invalid.")
                                }
                            }
                        )
                        OutlinedTextField(
                            password,
                            onValueChange = {
                                password = it
                                isPasswordValid = (it.length >= 8)
                            },
                            singleLine = true,
                            label = { Text("Password") },
                            isError = !isPasswordValid,
                            visualTransformation = PasswordVisualTransformation(),
                            supportingText = {
                                if (!isPasswordValid) {
                                    Text("Password must be at least 8 characters.")
                                }
                            }
                        )
                        OutlinedTextField(
                            repeatPassword,
                            onValueChange = {
                                repeatPassword = it
                                isRepeatPasswordValid = (it == password)
                            },
                            singleLine = true,
                            label = { Text("Repeat Password") },
                            isError = !isRepeatPasswordValid,
                            visualTransformation = PasswordVisualTransformation(),
                            supportingText = {
                                if (!isRepeatPasswordValid) {
                                    Text("Repeated password does not match password.")
                                }
                            }
                        )
                    }
                    Button(onClick = { signup() }) {
                        Text("Sign up!")
                    }
                    Column (
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Already have an account?")
                        Text(
                            buildAnnotatedString {
                                withLink(
                                    LinkAnnotation.Clickable(
                                        "",
                                        TextLinkStyles(style = SpanStyle(color = Color.Blue)),
                                        linkInteractionListener = {
                                            navController.navigate(Screens.LOGIN.route)
                                        }
                                    )
                                ) {
                                    append("Login!")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}