package com.japp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns;
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.japp.api.Credentials
import com.japp.api.CredentialsStorage
import com.japp.api.ErrorUtils
import com.japp.api.RetrofitClient
import com.japp.api.SessionManager
import com.japp.api.responses.auth.AuthResponse
import com.japp.api.responses.auth.LoginRequest
import com.japp.api.responses.auth.SignupRequest
import com.japp.messaging.JappMessagingService
import com.japp.ui.theme.JappTheme
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Date

class StartupActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        RetrofitClient.init(applicationContext)
        SessionManager.init(applicationContext)

        enableEdgeToEdge()
        val token = CredentialsStorage.load(this)
        if (token != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        } else {
            setContent {
                JappTheme {
                    StartupPage(this)
                }
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

@Preview(showSystemUi = true)
@Composable
fun StartupPage(context: Context? = null) {
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
fun LoginScreen(context: Context?, navController: NavController) {
    val coroutineScope = rememberCoroutineScope()

    var emailOrUsername by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var isValid by remember { mutableStateOf(false) }

    suspend fun login() {
        val res = RetrofitClient.authService.login(
            LoginRequest(
                emailOrUsername,
                password
            )
        )
        val body = res.body()
        Log.d("Tag", body.toString())

        if (body != null && res.isSuccessful) {
            val token = body.token
            val expiresAt = Date(System.currentTimeMillis() + (600 * 1000)) // 600 seconds as none is given with request?
            CredentialsStorage.save(
                context!!,
                Credentials(
                    accessToken = token,
                    expiresAt = expiresAt,
                    userId = body.user.id
                )
            )

            JappMessagingService.refreshToken(context)

            val intent = Intent(context, MainActivity::class.java)
            context.startActivity(intent)
        } else {
            val errorResponse = ErrorUtils.parseError(res)
            error = if (errorResponse != null) {
                "Login failed: ${errorResponse.message}"
            } else {
                "Login failed: ${res.code()} - ${res.message()}"
            }
            isValid = true
        }
    }

    Scaffold { innerPadding ->
        Column (
            Modifier.padding(innerPadding).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Login", style = MaterialTheme.typography.displaySmall)
            Box(Modifier.clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceContainer).padding(10.dp)) {
                Column (
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Column (

                    ) {
                        OutlinedTextField(
                            emailOrUsername,
                            onValueChange = { newText -> emailOrUsername = newText},
                            singleLine = true,
                            label = { Text("Username or Email") },
                            isError = isValid,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                        )
                        OutlinedTextField(
                            password,
                            onValueChange = { newText -> password = newText},
                            singleLine = true,
                            label = { Text("Password") },
                            isError = isValid,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                        )
                    }
                    if (error != null) {
                        Text(error!!, color = Color.Red)
                    }
                    Button(onClick = { coroutineScope.launch { login() } }) {
                        Text("Login")
                    }
                }
            }
            Text(
                buildAnnotatedString {
                    append("Don't have an account? ")
                    withLink(LinkAnnotation.Clickable(
                        tag = "Sign Up",
                        styles = TextLinkStyles(
                            style = SpanStyle(color = MaterialTheme.colorScheme.primary)
                        ),
                    ) {
                        navController.navigate(Screens.SIGNUP.route)
                    }) {
                        append("Sign Up")
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignupScreen(context: Context?, navController: NavController) {
    val coroutineScope = rememberCoroutineScope()
    val innerNavController = rememberNavController()
    val navBackStackEntry by innerNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    var error by remember { mutableStateOf<String?>(null) }

    val username = remember { mutableStateOf("") }
    val firstname = remember { mutableStateOf("") }
    val lastname = remember { mutableStateOf("") }
    val email = remember { mutableStateOf("") }
    val phone = remember { mutableStateOf(0) }
    val password = remember { mutableStateOf("") }
    val repeatPassword = remember { mutableStateOf("") }

    val isUsernameValid = remember { mutableStateOf(false) }
    val isFirstNameValid = remember { mutableStateOf(false) }
    val isLastNameValid = remember { mutableStateOf(false) }
    val isEmailValid = remember { mutableStateOf(false) }
    val isPhoneValid = remember { mutableStateOf(true) }
    val isPasswordValid = remember { mutableStateOf(false) }
    val isRepeatPasswordValid = remember { mutableStateOf(false) }
    val isAllValid = remember { mutableStateOf(false) }

    val isUsernameTouched = remember { mutableStateOf(false) }
    val isFirstNameTouched = remember { mutableStateOf(false) }
    val isLastNameTouched = remember { mutableStateOf(false) }
    val isEmailTouched = remember { mutableStateOf(false) }
    val isPhoneTouched = remember { mutableStateOf(false) }
    val isPasswordTouched = remember { mutableStateOf(false) }
    val isRepeatPasswordTouched = remember { mutableStateOf(false) }

    fun checkValid() {
        isAllValid.value = isUsernameValid.value
                && isFirstNameValid.value
                && isLastNameValid.value
                && isEmailValid.value
                && isPhoneValid.value
                && isPasswordValid.value
                && isRepeatPasswordValid.value
    }

    suspend fun signup() {
        val res = RetrofitClient.authService.signup(
            SignupRequest(
                username = username.value,
                email = email.value,
                password = password.value,
                firstname = firstname.value,
                lastname = lastname.value,
                phone = phone.value.toString()
            )
        )
        val body = res.body()

        if (body != null && res.isSuccessful) {
            val token = body.token
            val expiresAt = Date(System.currentTimeMillis() + (600 * 1000))
            CredentialsStorage.save(
                context!!,
                Credentials(
                    accessToken = token,
                    expiresAt = expiresAt,
                    userId = body.user.id
                )
            )

            JappMessagingService.refreshToken(context)

            val intent = Intent(context, MainActivity::class.java)
            context.startActivity(intent)
        } else {
            val errorResponse = ErrorUtils.parseError(res)
            error = if (errorResponse != null) {
                "Signup failed: ${errorResponse.message}"
            } else {
                "Signup failed: ${res.code()} - ${res.message()}"
            }
        }
    }

    Scaffold { innerPadding ->
        Column (
            Modifier.padding(innerPadding).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Sign Up", style = MaterialTheme.typography.displaySmall)
            SecondaryTabRow(
                selectedTabIndex = if (currentRoute == "1") 0 else 1,
                modifier = Modifier.widthIn(max = 220.dp)
            ) {
                Tab(
                    selected = currentRoute == "1",
                    onClick = { innerNavController.navigate("1") },
                    text = { Text("Personal info") }
                )
                Tab(
                    selected = currentRoute == "2",
                    onClick = { innerNavController.navigate("2") },
                    text = { Text("Credentials") }
                )
            }
            Box(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .animateContentSize()
                    .padding(10.dp)
            ) {
                NavHost(
                    navController = innerNavController,
                    startDestination = "1",
                    enterTransition = { fadeIn() + slideInHorizontally { it } },
                    exitTransition = { fadeOut() + slideOutHorizontally { -it } },
                    popEnterTransition = { fadeIn() + slideInHorizontally { -it } },
                    popExitTransition = { fadeOut() + slideOutHorizontally { it } }
                ) {
                    composable("1") {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            OutlinedTextField(
                                firstname.value,
                                onValueChange = {
                                    firstname.value = it
                                    isFirstNameTouched.value = true
                                    isFirstNameValid.value = (it.isNotEmpty())
                                    checkValid()
                                },
                                singleLine = true,
                                label = { Text("First name") },
                                isError = isFirstNameTouched.value && !isFirstNameValid.value,
                                supportingText = {
                                    if (isFirstNameTouched.value && !isFirstNameValid.value) {
                                        Text("First name must not be empty")
                                    }
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                            )
                            OutlinedTextField(
                                lastname.value,
                                onValueChange = {
                                    lastname.value = it
                                    isLastNameTouched.value = true
                                    isLastNameValid.value = (it.isNotEmpty())
                                    checkValid()
                                },
                                singleLine = true,
                                label = { Text("Last name") },
                                isError = isLastNameTouched.value && !isLastNameValid.value,
                                supportingText = {
                                    if (isLastNameTouched.value && !isLastNameValid.value) {
                                        Text("Last name must not be empty")
                                    }
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                            )
                            OutlinedTextField(
                                email.value,
                                onValueChange = {
                                    email.value = it
                                    isEmailTouched.value = true
                                    isEmailValid.value = Patterns.EMAIL_ADDRESS.matcher(it).matches()
                                    checkValid()
                                },
                                singleLine = true,
                                label = { Text("Email") },
                                isError = isEmailTouched.value && !isEmailValid.value,
                                supportingText = {
                                    if (isEmailTouched.value && !isEmailValid.value) {
                                        Text("Email is invalid.")
                                    }
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                            )
                            OutlinedTextField(
                                if(phone.value == 0) "" else phone.value.toString(),
                                onValueChange = {
                                    isPhoneTouched.value = true
                                    var input = it.trim()
                                    if (input == "") {
                                        phone.value = 0
                                        isPhoneValid.value = true
                                    }
                                    else if (input.matches(Regex("^\\d+\$"))) {
                                        if (input.length > 8) {
                                            input = it.substring(0, 8)
                                        }
                                        phone.value = input.toInt()
                                        isPhoneValid.value = phone.value.toString().length == 8
                                    } else {
                                        isPhoneValid.value = false
                                    }
                                    checkValid()
                                },
                                singleLine = true,
                                label = { Text("Phone") },
                                isError = isPhoneTouched.value && !isPhoneValid.value,
                                visualTransformation = PhoneNumberTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                supportingText = {
                                    if (isPhoneTouched.value && !isPhoneValid.value) {
                                        Text("Phone is not a valid number.")
                                    }
                                },
                            )
                            Button(onClick = { innerNavController.navigate("2") }) {
                                Text("Next")
                            }
                        }
                    }
                    composable ("2") {
                        Column (
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            OutlinedTextField(
                                username.value,
                                onValueChange = {
                                    username.value = it
                                    isUsernameTouched.value = true
                                    isUsernameValid.value = (it.length >= 3)
                                    checkValid()
                                },
                                singleLine = true,
                                label = { Text("Username") },
                                isError = isUsernameTouched.value && !isUsernameValid.value,
                                supportingText = {
                                    if (isUsernameTouched.value && !isUsernameValid.value) {
                                        Text("Username must be at least 3 characters")
                                    }
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Unspecified)
                            )
                            OutlinedTextField(
                                password.value,
                                onValueChange = {
                                    password.value = it
                                    isPasswordTouched.value = true
                                    isPasswordValid.value = it.matches("^((?=\\S*?[a-z])(?=\\S*?[0-9]).{8,})\$".toRegex()) // what the fuck
                                    checkValid()
                                },
                                singleLine = true,
                                label = { Text("Password") },
                                isError = isPasswordTouched.value && !isPasswordValid.value,
                                visualTransformation = PasswordVisualTransformation(),
                                supportingText = {
                                    if (isPasswordTouched.value && !isPasswordValid.value) {
                                        Text("Password must contain at least 1 letter, 1 number and be 8 long.")
                                    }
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                            )
                            OutlinedTextField(
                                repeatPassword.value,
                                onValueChange = {
                                    repeatPassword.value = it
                                    isRepeatPasswordTouched.value = true
                                    isRepeatPasswordValid.value = (it == password.value)
                                    checkValid()
                                },
                                singleLine = true,
                                label = { Text("Repeat Password") },
                                isError = isRepeatPasswordTouched.value && !isRepeatPasswordValid.value,
                                visualTransformation = PasswordVisualTransformation(),
                                supportingText = {
                                    if (isRepeatPasswordTouched.value && !isRepeatPasswordValid.value) {
                                        Text("Repeated password does not match password.")
                                    }
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                            )
                            Row (
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Button(onClick = { innerNavController.navigate("1") }) {
                                    Text("Go Back")
                                }
                                Button(onClick = { coroutineScope.launch { signup() } }) {
                                    Text("Sign Up")
                                }
                            }
                        }
                    }
                }
            }
            Text(
                buildAnnotatedString {
                    append("Already have an account? ")
                    withLink(LinkAnnotation.Clickable(
                        tag = "Login",
                        styles = TextLinkStyles(
                            style = SpanStyle(color = MaterialTheme.colorScheme.primary)
                        ),
                    ) {
                        navController.navigate(Screens.LOGIN.route)
                    }) {
                        append("Login")
                    }
                }
            )
        }
    }
}

fun String.addSpaces(): AnnotatedString {
    // Remove existing spaces and only keep digits
    val digitsOnly = this.filter { it.isDigit()}

    // Add spaces after every 2 digits
    return buildAnnotatedString {
        digitsOnly.chunked(2).forEachIndexed { index, chunk ->
            if (index > 0) {
                // Add space after every 2 digits
                append(" ")
            }
            append(chunk)
        }
    }
}

class PhoneNumberTransformation : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        // If the input text is greater than 8, only take the first 8 characters else, simply assign
        // the entire text.text to the trimmedText variable.
        // This is because our special code (excluding the dashes) has a max length of 8
        val trimmedText = if(text.text.length > 8) text.text.substring(0..7) else text.text
        // Add dashes to trimmedText as per required
        val addedDashes = trimmedText.addSpaces()
        // Return a TransformedText as the final result
        return TransformedText(
            // addedDashes being the now transformed text as an AnnotatedString
            text = addedDashes,
            // OffsetMapping object used for mapping original text positions to transformed text
            // positions.
            offsetMapping = object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int {
                    if (offset <= 2) return offset
                    if (offset <= 4) return offset + 1
                    if (offset <= 6) return offset + 2
                    if (offset <= 8) return offset + 3
                    return 10
                }

                override fun transformedToOriginal(offset: Int): Int {
                    if (offset <= 2) return offset
                    if (offset <= 5) return offset - 1
                    if (offset <= 8) return offset - 2
                    if (offset <= 10) return offset - 3
                    return 8
                }
            }
        )
    }
}
