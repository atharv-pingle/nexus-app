package com.nexus.planner

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
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
import com.nexus.planner.ui.tasks.TasksScreen
import com.nexus.planner.ui.tasks.TaskAlarmOverlay
import com.nexus.planner.ui.stats.StatsScreen
import com.nexus.planner.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
class MainActivity : ComponentActivity() {

    private val requestAudioPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

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
                    val pagerState = rememberPagerState(pageCount = { 3 })
                    val coroutineScope = rememberCoroutineScope()
                    val chatState by chatViewModel.uiState.collectAsState()
                    val pivotTitles = listOf("chat", "tasks", "stats")

                    Box(modifier = Modifier.fillMaxSize()) {
                        Scaffold(
                            topBar = {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Black)
                                ) {
                                    // Status / Profile Bar
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(42.dp),
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

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Text(
                                                    text = "SYNC",
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Zinc400,
                                                    modifier = Modifier
                                                        .clickable { chatViewModel.loadData() }
                                                        .padding(4.dp)
                                                )
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
                                    }

                                    // Windows Phone Metro Pivot Glanceable Typography Header
                                    val headerOffset = ((-80f) * (pagerState.currentPage + pagerState.currentPageOffsetFraction)).dp
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Black)
                                            .padding(top = 8.dp, bottom = 8.dp)
                                            .clipToBounds()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .offset(x = headerOffset)
                                                .padding(start = 16.dp),
                                            horizontalArrangement = Arrangement.spacedBy(28.dp),
                                            verticalAlignment = Alignment.Bottom
                                        ) {
                                            pivotTitles.forEachIndexed { index, title ->
                                                val isSelected = pagerState.currentPage == index
                                                Text(
                                                    text = title,
                                                    fontSize = 32.sp,
                                                    fontFamily = FontFamily.Default,
                                                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Light,
                                                    letterSpacing = (-0.5).sp,
                                                    color = if (isSelected) Color.White else Zinc600,
                                                    modifier = Modifier
                                                        .clickable {
                                                            coroutineScope.launch {
                                                                pagerState.animateScrollToPage(index)
                                                            }
                                                        }
                                                )
                                            }
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(1.dp)
                                            .background(Zinc900)
                                    )
                                }
                            },
                            containerColor = Black
                        ) { innerPadding ->
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                            ) { page ->
                                when (page) {
                                    0 -> ChatAssistantScreen(
                                        viewModel = chatViewModel,
                                        onStartVoiceRecognition = {
                                            voiceManager.startListening { transcript ->
                                                chatViewModel.updateLiveSpeech(transcript)
                                            }
                                        }
                                    )
                                    1 -> TasksScreen(
                                        todayTasks = chatState.todaySchedule,
                                        tomorrowTasks = chatState.tomorrowSchedule,
                                        isAutoPlanEnabled = chatState.isAutoPlanEnabled,
                                        isLoading = chatState.isLoading,
                                        onToggleTask = { blockId, isTomorrow -> chatViewModel.toggleTask(blockId, isTomorrow) },
                                        onToggleAutoPlan = { chatViewModel.toggleAutoPlan() },
                                        onTriggerAlarm = { task -> chatViewModel.triggerAlarmForTask(task) },
                                        onQuickAddTask = { title, isTomorrow -> chatViewModel.createQuickTask(title, isTomorrow = isTomorrow) },
                                        onDeleteTask = { blockId -> chatViewModel.deleteTask(blockId) },
                                        onUpdateTask = { blockId, title, startTime, duration, isTomorrow ->
                                            chatViewModel.updateTask(blockId, title, startTime, duration, isTomorrow)
                                        }
                                    )
                                    2 -> StatsScreen(
                                        pushupsCount = chatState.pushupsCount,
                                        workoutStreak = chatState.workoutStreak
                                    )
                                }
                            }
                        }

                        // Full-Screen Task Alarm Modal Overlay (Pops up anywhere across the app)
                        chatState.activeAlarmTask?.let { alarmTask ->
                            TaskAlarmOverlay(
                                task = alarmTask,
                                onStartTask = { chatViewModel.dismissAlarm() },
                                onSnooze = { mins -> chatViewModel.snoozeAlarm(mins) },
                                onVoiceInstruction = { cmd -> chatViewModel.handleTaskVoiceInstruction(alarmTask.id, cmd) },
                                onStartVoiceRecognition = {
                                    voiceManager.startListening { transcript ->
                                        chatViewModel.handleTaskVoiceInstruction(alarmTask.id, transcript)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
