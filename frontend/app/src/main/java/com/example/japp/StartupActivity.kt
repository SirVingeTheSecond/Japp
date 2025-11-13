package com.example.japp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns;
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import com.example.japp.api.Credentials
import com.example.japp.api.CredentialsStorage
import com.example.japp.api.ErrorUtils
import com.example.japp.api.RetrofitClient
import com.example.japp.api.responses.auth.AuthResponse
import com.example.japp.api.responses.auth.LoginRequest
import com.example.japp.api.responses.auth.SignupRequest
import com.example.japp.ui.theme.JappTheme
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Date

class StartupActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        RetrofitClient.init(applicationContext)

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
    var emailOrUsername by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var isValid by remember { mutableStateOf(false) }

    fun login() {
        val call: Call<AuthResponse?>? = RetrofitClient.authService.login(
            LoginRequest(
                emailOrUsername,
                password
            )
        )
        call!!.enqueue(object : Callback<AuthResponse?> {
            override fun onResponse(
                call: Call<AuthResponse?>,
                response: Response<AuthResponse?>
            ) {
                val body = response.body()
                Log.d("Tag", body.toString())

                if (body != null && response.isSuccessful) {
                    val token = body.token ?: return
                    val expiresAt =
                        Date(System.currentTimeMillis() + (600 * 1000)) // 600 seconds as none is given with request?
                    CredentialsStorage.save(context!!, Credentials(token, expiresAt))

                    val intent = Intent(context, MainActivity::class.java)
                    context.startActivity(intent)
                } else {
                    val errorResponse = ErrorUtils.parseError(response)
                    error = "Login failed: ${errorResponse!!.message}"
                    isValid = true
                }
            }

            override fun onFailure(call: Call<AuthResponse?>, t: Throwable) {
                Log.d("Tag", t.message!!)
                isValid = true
            }
        })
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
                            emailOrUsername,
                            onValueChange = { newText -> emailOrUsername = newText},
                            singleLine = true,
                            label = { Text("Username or Email") },
                            isError = isValid
                        )
                        OutlinedTextField(
                            password,
                            onValueChange = { newText -> password = newText},
                            singleLine = true,
                            label = { Text("Password") },
                            isError = isValid,
                            visualTransformation = PasswordVisualTransformation()
                        )
                    }
                    error?.let { Text(it) }
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
fun SignupScreen(context: Context?, navController: NavController) {
    var email = remember { mutableStateOf("") }
    var username = remember { mutableStateOf("") }
    var firstname = remember { mutableStateOf("") }
    var lastname = remember { mutableStateOf("") }
    var phone = remember { mutableIntStateOf(0) }
    var password = remember { mutableStateOf("") }
    var repeatPassword = remember { mutableStateOf("") }

    var isValid = remember { mutableStateOf(false) }

    var error by remember { mutableStateOf<String?>(null) }

    fun signup() {
        if (isValid.value) {
            // Send request to sign up here?
            val call: Call<AuthResponse?>? = RetrofitClient.authService.signup(SignupRequest(
                    email.value,
                    username.value,
                    firstname.value,
                    lastname.value,
                    password.value,
                    phone.intValue.toString()
                )
            )
            call!!.enqueue(object : Callback<AuthResponse?> {
                override fun onResponse(call: Call<AuthResponse?>, response: Response<AuthResponse?>) {

                    // we are getting response from our body
                    // and passing it to our modal class.
                    val body = response.body()
                    if (body != null && response.isSuccessful) {
                        navController.navigate(Screens.LOGIN.route)
                    } else {
                        val errorResponse = ErrorUtils.parseError(response)
                        error = "Signup failed: ${errorResponse!!.message}"
                    }
                }

                override fun onFailure(call: Call<AuthResponse?>, t: Throwable) {
                    Log.d("Tag", t.message!!)
                }
            })
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
            Box(
                Modifier.clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(10.dp)
            ) {
                Column (
                    Modifier.widthIn(0.dp, 280.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SignupTabs(
                        email,
                        username,
                        firstname,
                        lastname,
                        phone,
                        password,
                        repeatPassword,
                        isValid,
                        { signup() },
                    )
                    error?.let { Text(it) }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignupTabs(
    email: MutableState<String>,
    username: MutableState<String>,
    firstname: MutableState<String>,
    lastname: MutableState<String>,
    phone: MutableState<Int>,
    password: MutableState<String>,
    repeatPassword: MutableState<String>,
    isValid: MutableState<Boolean>,
    onSignup: () -> Unit,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()

    var isEmailValid = remember { mutableStateOf(true) }
    var isUsernameValid = remember { mutableStateOf(true) }
    var isFirstNameValid = remember { mutableStateOf(true) }
    var isLastNameValid = remember { mutableStateOf(true) }
    var isPhoneValid = remember { mutableStateOf(true) }
    var isPasswordValid = remember { mutableStateOf(true) }
    var isRepeatPasswordValid = remember { mutableStateOf(true) }

    fun checkValid() {
        if (
            isEmailValid.value &&
            isUsernameValid.value &&
            isFirstNameValid.value &&
            isLastNameValid.value &&
            isPhoneValid.value &&
            isPasswordValid.value &&
            isRepeatPasswordValid.value
        ) {
            isValid.value = true
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val selectedDestination = when (currentRoute) {
        "1" -> 0
        "2" -> 1
        else -> 0
    }

    Column(
        modifier = modifier
    ) {
        SecondaryTabRow(
            selectedTabIndex = selectedDestination,
        ) {
            Tab(
                selected = selectedDestination == 0,
                onClick = {
                    navController.navigate("1")
                },
                Modifier.padding(10.dp)
            ) {
                Text("1")
            }
            Tab(
                selected = selectedDestination == 1,
                onClick = {
                    navController.navigate("2")
                }
            ) {
                Text("2")
            }
        }
        NavHost(
            navController = navController,
            startDestination = "1"
        ) {
            composable ("1") {
                Column (
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    OutlinedTextField(
                        firstname.value,
                        onValueChange = {
                            firstname.value = it
                            isFirstNameValid.value = (it.isNotEmpty())
                            checkValid()
                        },
                        singleLine = true,
                        label = { Text("First name") },
                        isError = !isFirstNameValid.value,
                        supportingText = {
                            if (!isFirstNameValid.value) {
                                Text("First name not be empty")
                            }
                        }
                    )
                    OutlinedTextField(
                        lastname.value,
                        onValueChange = {
                            lastname.value = it
                            isLastNameValid.value = (it.isNotEmpty())
                            checkValid()
                        },
                        singleLine = true,
                        label = { Text("Last name") },
                        isError = !isLastNameValid.value,
                        supportingText = {
                            if (!isLastNameValid.value) {
                                Text("Last name must not be empty")
                            }
                        }
                    )
                    OutlinedTextField(
                        email.value,
                        onValueChange = {
                            email.value = it
                            isEmailValid.value = Patterns.EMAIL_ADDRESS.matcher(it).matches()
                            checkValid()
                        },
                        singleLine = true,
                        label = { Text("Email") },
                        isError = !isEmailValid.value,
                        supportingText = {
                            if (!isEmailValid.value) {
                                Text("Email is invalid.")
                            }
                        }
                    )
                    OutlinedTextField(
                        if(phone.value == 0) "" else phone.value.toString(),
                        onValueChange = {
                            var input = it.trim()
                            if (input == "") {
                                phone.value = 0
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
                        isError = !isPhoneValid.value,
                        visualTransformation = PhoneNumberTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        supportingText = {
                            if (!isPhoneValid.value) {
                                Text("Phone is not a valid number.")
                            }
                        },
                    )
                    Button(onClick = { navController.navigate("2") }) {
                        Text("Next!")
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
                            isUsernameValid.value = (it.length >= 3)
                            checkValid()
                        },
                        singleLine = true,
                        label = { Text("Username") },
                        isError = !isUsernameValid.value,
                        supportingText = {
                            if (!isUsernameValid.value) {
                                Text("Username must be at least 3 characters")
                            }
                        }
                    )
                    OutlinedTextField(
                        password.value,
                        onValueChange = {
                            password.value = it
                            isPasswordValid.value = it.matches("^((?=\\S*?[a-z])(?=\\S*?[0-9]).{8,})\$".toRegex())
                            checkValid()
                        },
                        singleLine = true,
                        label = { Text("Password") },
                        isError = !isPasswordValid.value,
                        visualTransformation = PasswordVisualTransformation(),
                        supportingText = {
                            if (!isPasswordValid.value) {
                                Text("Password must contain at least 1 letter, 1 number and be 8 long.")
                            }
                        }
                    )
                    OutlinedTextField(
                        repeatPassword.value,
                        onValueChange = {
                            repeatPassword.value = it
                            isRepeatPasswordValid.value = (it == password.value)
                            checkValid()
                        },
                        singleLine = true,
                        label = { Text("Repeat Password") },
                        isError = !isRepeatPasswordValid.value,
                        visualTransformation = PasswordVisualTransformation(),
                        supportingText = {
                            if (!isRepeatPasswordValid.value) {
                                Text("Repeated password does not match password.")
                            }
                        }
                    )
                    Row (
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(onClick = { navController.navigate("1") }) {
                            Text("Go back!")
                        }
                        Button(onClick = onSignup) {
                            Text("Sign up!")
                        }
                    }
                }
            }
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