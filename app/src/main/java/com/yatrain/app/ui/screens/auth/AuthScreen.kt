package com.yatrain.app.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yatrain.app.i18n.LocalStrings
import com.yatrain.app.ui.theme.AppNavy
import com.yatrain.app.ui.theme.AppOrange

enum class AuthMode { SIGN_IN, SIGN_UP, RESET_PASSWORD }

@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    onAuthSuccess: () -> Unit
) {
    val s = LocalStrings.current
    var mode by remember { mutableStateOf(AuthMode.SIGN_IN) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }

    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    LaunchedEffect(currentUser) {
        if (currentUser != null) onAuthSuccess()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppNavy)
            .systemBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(60.dp))

            // Logo / Brand
            Text(
                text = "Yatrain",
                fontSize = 36.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 1.sp
            )
            Text(
                text = s.authTagline,
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.6f)
            )

            Spacer(Modifier.height(48.dp))

            // Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(Modifier.padding(24.dp)) {

                    Text(
                        text = when (mode) {
                            AuthMode.SIGN_IN        -> s.authSignIn
                            AuthMode.SIGN_UP        -> s.authCreateAccount
                            AuthMode.RESET_PASSWORD -> s.authResetPassword
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(20.dp))

                    if (mode == AuthMode.SIGN_UP) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text(s.authFullName) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(Modifier.height(12.dp))
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; viewModel.clearError() },
                        label = { Text(s.authEmail) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true
                    )

                    if (mode != AuthMode.RESET_PASSWORD) {
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it; viewModel.clearError() },
                            label = { Text(s.authPassword) },
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true
                        )
                    }

                    error?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                    }

                    Spacer(Modifier.height(20.dp))

                    Button(
                        onClick = {
                            when (mode) {
                                AuthMode.SIGN_IN -> viewModel.signIn(email, password, onAuthSuccess)
                                AuthMode.SIGN_UP -> viewModel.signUp(email, password, name, onAuthSuccess)
                                AuthMode.RESET_PASSWORD -> viewModel.resetPassword(
                                    email     = email,
                                    onSuccess = { mode = AuthMode.SIGN_IN }
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AppOrange),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                        } else {
                            Text(
                                text = when (mode) {
                                    AuthMode.SIGN_IN        -> s.authSignIn
                                    AuthMode.SIGN_UP        -> s.authCreateAccount
                                    AuthMode.RESET_PASSWORD -> s.authSendResetLink
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Toggle links
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        when (mode) {
                            AuthMode.SIGN_IN -> {
                                TextButton(onClick = { mode = AuthMode.SIGN_UP }) {
                                    Text(s.authCreateAccount, color = AppNavy)
                                }
                                TextButton(onClick = { mode = AuthMode.RESET_PASSWORD }) {
                                    Text(s.authForgotPassword, color = AppNavy)
                                }
                            }
                            AuthMode.SIGN_UP, AuthMode.RESET_PASSWORD -> {
                                TextButton(onClick = { mode = AuthMode.SIGN_IN }) {
                                    Text(s.authBackToSignIn, color = AppNavy)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
