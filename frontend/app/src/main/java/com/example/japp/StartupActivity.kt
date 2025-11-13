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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
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
    var error by remember { mutableStateOf<String?>(null) }
    var isValid by remember { mutableStateOf(false) }

    fun login() {
        val call: Call<AuthResponse?>? = RetrofitClient.authService.login(
            LoginRequest(
                username,
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
                    CredentialsStorage.save(context, Credentials(token, expiresAt))

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
                            username,
                            onValueChange = { newText -> username = newText},
                            singleLine = true,
                            label = { Text("Username") },
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
fun SignupScreen(context: Context, navController: NavController) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableIntStateOf(0) }
    var password by remember { mutableStateOf("") }
    var repeatPassword by remember { mutableStateOf("") }

    var isUsernameValid by remember { mutableStateOf(true) }
    var isEmailValid by remember { mutableStateOf(true) }
    var isPhoneValid by remember { mutableStateOf(true) }
    var isPasswordValid by remember { mutableStateOf(true) }
    var isRepeatPasswordValid by remember { mutableStateOf(true) }

    var error by remember { mutableStateOf<String?>(null) }

    val numberPattern = remember { Regex("^\\d+\$") }

    fun signup() {
        if (isUsernameValid && isEmailValid && isPhoneValid && isPasswordValid && isRepeatPasswordValid) {
            // Send request to sign up here?
            val call: Call<AuthResponse?>? = RetrofitClient.authService.signup(SignupRequest(
                    username,
                    email,
                    password,
                    phone.toString()
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
                        isUsernameValid = false
                        isEmailValid = false
                        isPasswordValid = false
                        isPhoneValid = false
                        isRepeatPasswordValid = false
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
                    Column (
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        OutlinedTextField(
                            username,
                            onValueChange = {
                                username = it
                                isUsernameValid = (it.length >= 2)
                            },
                            singleLine = true,
                            label = { Text("Username") },
                            isError = !isUsernameValid,
                            supportingText = {
                                if (!isUsernameValid) {
                                    Text("Username must be at least 2 characters")
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
                            if(phone == 0) "" else phone.toString(),
                            onValueChange = {
                                var input = it.trim()
                                if (input == "") {
                                    phone = 0
                                }
                                else if (input.matches(numberPattern)) {
                                    if (input.length > 8) {
                                        input = it.substring(0, 8)
                                    }
                                    phone = input.toInt()
                                    isPhoneValid = phone.toString().length == 8
                                } else {
                                    isPhoneValid = false
                                }
                            },
                            singleLine = true,
                            label = { Text("Phone") },
                            isError = !isPhoneValid,
                            visualTransformation = PhoneNumberTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            supportingText = {
                                if (!isPhoneValid) {
                                    Text("Phone is not a valid number.")
                                }
                            },
                        )
                        OutlinedTextField(
                            password,
                            onValueChange = {
                                password = it
//                                isPasswordValid = (it.length >= 8)
                                // Check with ^((?=\S*?[a-z])(?=\S*?[0-9]).{8,})$
                                isPasswordValid = it.matches("^((?=\\S*?[a-z])(?=\\S*?[0-9]).{8,})\$".toRegex())
                            },
                            singleLine = true,
                            label = { Text("Password") },
                            isError = !isPasswordValid,
                            visualTransformation = PasswordVisualTransformation(),
                            supportingText = {
                                if (!isPasswordValid) {
                                    Text("Password must contain at least 1 letter, 1 number and be 8 long.")
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
                    error?.let { Text(it) }
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