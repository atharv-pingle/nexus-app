package com.nexus.planner

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nexus.planner.ui.architect.ArchitectScreen
import com.nexus.planner.ui.architect.ArchitectViewModel
import com.nexus.planner.ui.auth.LoginScreen
import com.nexus.planner.ui.coach.CoachScreen
import com.nexus.planner.ui.coach.CoachViewModel
import com.nexus.planner.ui.components.VoiceModalSheet
import com.nexus.planner.ui.navigation.Screen
import com.nexus.planner.ui.stats.StatsScreen
import com.nexus.planner.ui.stats.StatsViewModel
import com.nexus.planner.ui.theme.*
import com.nexus.planner.ui.timeline.TimelineScreen
import com.nexus.planner.ui.timeline.TimelineViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate

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
        val repository = app.repository
        val voiceManager = app.voiceManager

        // Sync with server if already logged in
        if (app.authManager.hasValidToken()) {
            lifecycleScope.launch {
                repository.syncWithServer(LocalDate.now().toString())
            }
        }

        val timelineViewModel = TimelineViewModel(repository)
        val architectViewModel = ArchitectViewModel(repository)
        val coachViewModel = CoachViewModel(repository)
        val statsViewModel = StatsViewModel(repository)

        setContent {
            NexusTheme {
                val isLoggedIn by app.authManager.isLoggedIn.collectAsState()

                if (!isLoggedIn) {
                    LoginScreen(
                        authManager = app.authManager,
                        apiClient = app.apiClient,
                        onLoginSuccess = {
                            lifecycleScope.launch {
                                repository.syncWithServer(LocalDate.now().toString())
                            }
                        }
                    )
                } else {
                    val navController = rememberNavController()
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Timeline.route
                    var showVoiceSheet by remember { mutableStateOf(false) }

                    Scaffold(
                        topBar = {
                            Surface(
                                modifier = Modifier.fillMaxWidth().height(42.dp),
                                color = Black,
                                border = androidx.compose.foundation.BorderStroke(1.dp, Zinc900)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "NEXUS // " + app.authManager.getUsername().uppercase(),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Zinc400
                                    )
                                    Text(
                                        text = "SIGN OUT",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        color = Zinc500,
                                        modifier = Modifier
                                            .clickable {
                                                lifecycleScope.launch {
                                                    repository.clearLocalData()
                                                    app.authManager.clearSession()
                                                }
                                            }
                                            .padding(4.dp)
                                    )
                                }
                            }
                        },
                        bottomBar = {
                            NexusBottomBar(
                                currentRoute = currentRoute,
                                onNavigate = { route ->
                                    navController.navigate(route) {
                                        popUpTo(Screen.Timeline.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                onMicClick = { showVoiceSheet = true }
                            )
                        },
                        containerColor = Black
                    ) { innerPadding ->
                        NavHost(
                            navController = navController,
                            startDestination = Screen.Timeline.route,
                            modifier = Modifier.padding(innerPadding)
                        ) {
                            composable(Screen.Timeline.route) {
                                TimelineScreen(viewModel = timelineViewModel)
                            }
                            composable(Screen.Architect.route) {
                                ArchitectScreen(viewModel = architectViewModel)
                            }
                            composable(Screen.Coach.route) {
                                CoachScreen(viewModel = coachViewModel)
                            }
                            composable(Screen.Stats.route) {
                                StatsScreen(viewModel = statsViewModel)
                            }
                        }

                        if (showVoiceSheet) {
                            VoiceModalSheet(
                                voiceManager = voiceManager,
                                onIntentConfirmed = { speechText ->
                                    showVoiceSheet = false
                                    lifecycleScope.launch {
                                        val result = repository.processVoiceIntent(speechText)
                                        result.onSuccess { intent ->
                                            if (intent.spokenConfirmation.isNotBlank()) {
                                                voiceManager.speak(intent.spokenConfirmation)
                                            }
                                        }
                                    }
                                },
                                onDismiss = { showVoiceSheet = false }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NexusBottomBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onMicClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(62.dp),
        color = Black,
        border = androidx.compose.foundation.BorderStroke(1.dp, Zinc900)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tab: Timeline
            NavTabItem(
                label = "Orbit",
                isSelected = currentRoute == Screen.Timeline.route,
                onClick = { onNavigate(Screen.Timeline.route) }
            )

            // Tab: Architect
            NavTabItem(
                label = "Plan",
                isSelected = currentRoute == Screen.Architect.route,
                onClick = { onNavigate(Screen.Architect.route) }
            )

            // Center Tactile Microphone Button
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(White)
                    .border(2.dp, Black, CircleShape)
                    .clickable(onClick = onMicClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Voice Mode",
                    tint = Black,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Tab: Coach
            NavTabItem(
                label = "Agent",
                isSelected = currentRoute == Screen.Coach.route,
                onClick = { onNavigate(Screen.Coach.route) }
            )

            // Tab: Stats
            NavTabItem(
                label = "Stats",
                isSelected = currentRoute == Screen.Stats.route,
                onClick = { onNavigate(Screen.Stats.route) }
            )
        }
    }
}

@Composable
fun NavTabItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Text(
        text = label,
        fontFamily = FontFamily.Monospace,
        fontSize = 12.sp,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
        color = if (isSelected) White else Zinc500,
        modifier = Modifier.clickable(onClick = onClick).padding(6.dp)
    )
}
