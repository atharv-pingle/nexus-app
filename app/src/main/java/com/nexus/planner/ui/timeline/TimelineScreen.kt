package com.nexus.planner.ui.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.planner.data.local.entity.ScheduleBlockEntity
import com.nexus.planner.ui.components.*
import com.nexus.planner.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun TimelineScreen(
    viewModel: TimelineViewModel,
    modifier: Modifier = Modifier
) {
    val schedule by viewModel.schedule.collectAsState()
    val habits by viewModel.habits.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    var showSkipFixSheet by remember { mutableStateOf(false) }
    var showNapModal by remember { mutableStateOf(false) }
    var showWorkoutModal by remember { mutableStateOf(false) }

    val activeBlock = schedule.firstOrNull { it.status == "ACTIVE" || it.status == "IN_PROGRESS" }
        ?: schedule.firstOrNull { it.status != "COMPLETED" && it.status != "SKIPPED" }
        ?: schedule.firstOrNull()

    val totalStreak = habits.maxOfOrNull { it.totalCompletions } ?: 14
    val currentDayIndex = habits.maxOfOrNull { it.currentDayIndex } ?: 19

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Black)
            .padding(horizontal = 16.dp)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = LocalDate.now().format(DateTimeFormatter.ofPattern("EEE dd MMM yyyy")).uppercase(),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = Zinc400
                        )
                        Text(
                            text = "NEXUS // TODAY",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = White
                        )
                    }
                    ChamferTag(text = "LIVE SYNC", colorAccent = EmeraldAccent)
                }
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Zinc900))
            }

            // 66-Day UCL Habit Automaticity Trajectory Arc
            item {
                JourneyArc(
                    currentDay = currentDayIndex,
                    targetDays = 66,
                    streak = totalStreak
                )
            }

            // Tactical Active Quest SystemPanel
            if (activeBlock != null) {
                item {
                    SystemPanel(
                        title = "ACTIVE QUEST // ${activeBlock.category}",
                        subtitle = "${activeBlock.startTime} – ${activeBlock.endTime} (${activeBlock.durationMinutes}m)",
                        cornerTag = if (activeBlock.status == "COMPLETED") "DONE" else "ACTIVE"
                    ) {
                        Text(
                            text = activeBlock.title,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = White,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        if (activeBlock.microAction.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Zinc900)
                                    .border(1.dp, Zinc800, RoundedCornerShape(4.dp))
                                    .padding(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                        Text(
                                            text = "2-MIN STARTER STEP:",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 9.sp,
                                            color = Zinc500
                                        )
                                        Text(
                                            text = activeBlock.microAction,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            color = Zinc200
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        // Tactical HUD Control Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ChamferButton(
                                text = "+15m",
                                onClick = { viewModel.addDelay(15) },
                                modifier = Modifier.weight(1f)
                            )
                            ChamferButton(
                                text = "+30m",
                                onClick = { viewModel.addDelay(30) },
                                modifier = Modifier.weight(1f)
                            )
                            ChamferButton(
                                text = "⚡ SKIP-FIX",
                                onClick = { showSkipFixSheet = true },
                                colorAccent = AmberAccent,
                                modifier = Modifier.weight(1.3f)
                            )
                            ChamferButton(
                                text = "DONE",
                                onClick = { viewModel.completeMicroAction(activeBlock.id) },
                                isPrimary = true,
                                colorAccent = EmeraldAccent,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Embedded Active Tool: 15:30 Transition Interceptor
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(ChamferCorner)
                        .background(Zinc950)
                        .border(1.dp, BlueAccent.copy(alpha = 0.5f), ChamferCorner)
                        .clickable { showNapModal = true }
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(BlueAccent))
                                Text(
                                    text = "15:30 WORK-TO-LIFE INTERCEPTOR",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = BlueAccent
                                )
                            }
                            Text(
                                text = "20m NSDR & Power Nap Reset",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = White,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                            Text(
                                text = "Tap to launch guided breathing ring & recovery timer.",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = Zinc500,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        ChamferButton(
                            text = "START",
                            onClick = { showNapModal = true },
                            isPrimary = true,
                            colorAccent = BlueAccent
                        )
                    }
                }
            }

            // Embedded Active Tool: Dumbbell Bench Workout Tracker
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(ChamferCorner)
                        .background(Zinc950)
                        .border(1.dp, AmberAccent.copy(alpha = 0.5f), ChamferCorner)
                        .clickable { showWorkoutModal = true }
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(AmberAccent))
                                Text(
                                    text = "STRENGTH WORKOUT ACTIVE TOOL",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = AmberAccent
                                )
                            }
                            Text(
                                text = "Dumbbell & Bench Rest Counter",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = White,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                            Text(
                                text = "Auto-vibrating 60s/90s rest interval timer between sets.",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = Zinc500,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        ChamferButton(
                            text = "REST",
                            onClick = { showWorkoutModal = true },
                            isPrimary = true,
                            colorAccent = AmberAccent
                        )
                    }
                }
            }

            // Full Day Quest Stack
            item {
                Text(
                    text = "TODAY'S SCHEDULE STACK",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = Zinc500,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            items(schedule, key = { it.id }) { block ->
                TacticalScheduleRow(
                    block = block,
                    onComplete = { viewModel.completeMicroAction(block.id) }
                )
            }
        }

        // Notification Banner
        if (statusMessage != null) {
            Snackbar(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp),
                containerColor = Zinc900,
                contentColor = White,
                dismissAction = {
                    TextButton(onClick = { viewModel.clearStatusMessage() }) {
                        Text("OK", color = Zinc300, fontFamily = FontFamily.Monospace)
                    }
                }
            ) {
                Text(
                    text = statusMessage!!,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                )
            }
        }

        // Skip-Fix Bottom Sheet
        if (showSkipFixSheet && activeBlock != null) {
            SkipFixModalSheet(
                block = activeBlock,
                onGentleSwap = { b, counterAction ->
                    showSkipFixSheet = false
                    viewModel.applyGentleSwap(b.id, counterAction)
                },
                onShiftLater = { b ->
                    showSkipFixSheet = false
                    viewModel.addDelay(30)
                },
                onDismiss = { showSkipFixSheet = false }
            )
        }

        // 15:30 NSDR Nap Modal
        if (showNapModal) {
            TransitionNapModal(
                onDismiss = { showNapModal = false },
                onComplete = {
                    showNapModal = false
                    viewModel.completeMicroAction(
                        schedule.firstOrNull { it.category.contains("TRANSITION", true) || it.category.contains("SLEEP", true) }?.id ?: 0L
                    )
                }
            )
        }

        // Dumbbell Workout Rest Modal
        if (showWorkoutModal) {
            WorkoutRestModal(
                onDismiss = { showWorkoutModal = false }
            )
        }
    }
}

@Composable
fun TacticalScheduleRow(
    block: ScheduleBlockEntity,
    onComplete: () -> Unit
) {
    val isDone = block.status == "COMPLETED"
    val isSkipped = block.status == "SKIPPED"
    val isDelayed = block.status == "DELAYED"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(Zinc950)
            .border(1.dp, if (isDone) Zinc900 else Zinc800, RoundedCornerShape(4.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Tactical Checkbox
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (isDone) EmeraldAccent else Zinc900)
                        .border(1.dp, if (isDone) EmeraldAccent else Zinc700, RoundedCornerShape(3.dp))
                        .clickable(enabled = !isDone, onClick = onComplete),
                    contentAlignment = Alignment.Center
                ) {
                    if (isDone) {
                        Text("✓", color = Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Column {
                    Text(
                        text = "${block.startTime} - ${block.endTime} // ${block.category}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        color = if (isDone) Zinc600 else Zinc500
                    )
                    Text(
                        text = block.title,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = if (isDone) FontWeight.Normal else FontWeight.Medium,
                        color = if (isDone) Zinc600 else Zinc200
                    )
                }
            }

            ChamferTag(
                text = block.status,
                colorAccent = when {
                    isDone -> EmeraldAccent
                    isDelayed -> AmberAccent
                    isSkipped -> RedAccent
                    else -> Zinc400
                }
            )
        }
    }
}
