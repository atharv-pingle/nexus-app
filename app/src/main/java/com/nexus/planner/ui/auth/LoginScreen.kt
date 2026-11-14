package com.nexus.planner.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.planner.data.remote.AuthManager
import com.nexus.planner.data.remote.NexusApiClient
import com.nexus.planner.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    authManager: AuthManager,
    apiClient: NexusApiClient,
    onLoginSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var serverUrl by remember { mutableStateOf(authManager.getServerUrl()) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundPrimary)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Zinc800, RoundedCornerShape(8.dp))
                .background(SurfaceDark, RoundedCornerShape(8.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "NEXUS // CONNECT",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = Zinc100
                )
            )
            Text(
                text = "Self-Hosted Agentic Planner",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Zinc400,
                    fontSize = 11.sp
                ),
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
            )

            if (errorMessage != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .background(Color(0xFF2A1215), RoundedCornerShape(4.dp))
                        .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = Color(0xFFF87171),
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                    )
                }
            }

            // Server URL
            Text(
                text = "SERVER ENDPOINT (PORT 3066)",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    color = Zinc400,
                    fontSize = 10.sp
                ),
                modifier = Modifier.padding(bottom = 4.dp)
            )
            OutlinedTextField(
                value = serverUrl,
                onValueChange = { serverUrl = it; errorMessage = null },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("http://192.168.1.50:3066", color = Zinc600, fontSize = 12.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Zinc400,
                    unfocusedBorderColor = Zinc800,
                    focusedTextColor = Zinc100,
                    unfocusedTextColor = Zinc200
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Username
            Text(
                text = "USERNAME / EMAIL",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    color = Zinc400,
                    fontSize = 10.sp
                ),
                modifier = Modifier.padding(bottom = 4.dp)
            )
            OutlinedTextField(
                value = username,
                onValueChange = { username = it; errorMessage = null },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("e.g. admin or username", color = Zinc600, fontSize = 12.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Zinc400,
                    unfocusedBorderColor = Zinc800,
                    focusedTextColor = Zinc100,
                    unfocusedTextColor = Zinc200
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Password
            Text(
                text = "PASSWORD",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    color = Zinc400,
                    fontSize = 10.sp
                ),
                modifier = Modifier.padding(bottom = 4.dp)
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it; errorMessage = null },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                placeholder = { Text("••••••••", color = Zinc600, fontSize = 12.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Zinc400,
                    unfocusedBorderColor = Zinc800,
                    focusedTextColor = Zinc100,
                    unfocusedTextColor = Zinc200
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    focusManager.clearFocus()
                    if (serverUrl.isBlank() || username.isBlank() || password.isBlank()) {
                        errorMessage = "Please enter server URL, username, and password"
                        return@Button
                    }
                    isLoading = true
                    errorMessage = null
                    coroutineScope.launch {
                        val result = apiClient.login(serverUrl, username, password)
                        isLoading = false
                        result.fold(
                            onSuccess = {
                                onLoginSuccess()
                            },
                            onFailure = { error ->
                                errorMessage = error.message ?: "Authentication failed"
                            }
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Zinc100,
                    contentColor = BackgroundPrimary,
                    disabledContainerColor = Zinc800,
                    disabledContentColor = Zinc600
                ),
                shape = RoundedCornerShape(4.dp)
            ) {
                if (isLoading) {
                    Text(
                        text = "CONNECTING...",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                } else {
                    Text(
                        text = "AUTHENTICATE",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
