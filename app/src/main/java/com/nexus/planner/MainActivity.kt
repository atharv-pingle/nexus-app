package com.nexus.planner

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.nexus.planner.ui.auth.LoginScreen
import com.nexus.planner.ui.coach.ChatAssistantScreen
import com.nexus.planner.ui.coach.ChatAssistantViewModel
import com.nexus.planner.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val requestAudioPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestAudioPermission.launch(Manifest.permission.RECORD_AUDIO)
        }

        val app = application as NexusApp
        val voiceManager = app.voiceManager
        val chatViewModel = ChatAssistantViewModel(app.apiClient)

        // Live Background Agent Loop (checks every 60s for proactive interventions)
        lifecycleScope.launch {
            while (isActive) {
                delay(60_000)
                if (app.authManager.hasValidToken()) {
                    chatViewModel.checkProactiveNudge()
                }
            }
        }

        setContent {
            NexusTheme {
                val isLoggedIn by app.authManager.isLoggedIn.collectAsState()

                if (!isLoggedIn) {
                    LoginScreen(
                        authManager = app.authManager,
                        apiClient = app.apiClient,
                        onLoginSuccess = {
                            chatViewModel.loadData()
                        }
                    )
                } else {
                    Scaffold(
                        topBar = {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                                color = Black,
                                border = BorderStroke(1.dp, Zinc900)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(Emerald400)
                                        )
                                        Text(
                                            text = "NEXUS // " + app.authManager.getUsername().uppercase(),
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Zinc300
                                        )
                                    }

                                    Text(
                                        text = "SIGN OUT",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        color = Zinc500,
                                        modifier = Modifier
                                            .clickable {
                                                lifecycleScope.launch {
                                                    app.authManager.clearSession()
                                                }
                                            }
                                            .padding(4.dp)
                                    )
                                }
                            }
                        },
                        containerColor = Black
                    ) { innerPadding ->
                        ChatAssistantScreen(
                            viewModel = chatViewModel,
                            onStartVoiceRecognition = {
                                voiceManager.startListening { transcript ->
                                    chatViewModel.updateLiveSpeech(transcript)
                                }
                            },
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}
