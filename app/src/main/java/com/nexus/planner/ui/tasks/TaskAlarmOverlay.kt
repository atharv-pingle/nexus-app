package com.nexus.planner.ui.tasks

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.planner.data.remote.ServerScheduleBlockDto
import com.nexus.planner.ui.theme.*

@Composable
fun TaskAlarmOverlay(
    task: ServerScheduleBlockDto,
    onStartTask: () -> Unit,
    onSnooze: (Int) -> Unit,
    onVoiceInstruction: (String) -> Unit,
    onStartVoiceRecognition: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contextualIcon = getContextualTaskIcon(task.title, task.block_type)

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Surface(
        color = Color.Black.copy(alpha = 0.98f),
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Status Bar: Sleek & Executive
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "NEXUS // ALARM",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = Zinc400
                )

                Text(
                    text = task.start_time,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Center Display: Pulsing Glow, Contextual Icon, Title & Dedicated Voice Mic
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                // Pulsing Halo Ring
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(Emerald400.copy(alpha = 0.15f))
                        .border(1.dp, Emerald400.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(Emerald400),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = contextualIcon,
                            fontSize = 36.sp
                        )
                    }
                }

                // Due Time Badge & Title
                Surface(
                    color = Emerald400.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Emerald400.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "DUE NOW • ${task.start_time}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Emerald400,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }

                Text(
                    text = task.title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    lineHeight = 30.sp
                )

                // Dedicated Context-Aware Mic
                Box(
                    modifier = Modifier
                        .padding(top = 14.dp)
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Zinc900)
                        .border(1.dp, Emerald400, CircleShape)
                        .clickable { onStartVoiceRecognition() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Voice Instruction",
                        tint = Emerald400,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            // Bottom Actions: Start vs Snooze
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Primary: Start Task
                Surface(
                    color = Emerald400,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onStartTask() }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "✓ I'M ON IT — START TASK",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }

                // Secondary: Snooze +15m / +30m
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        color = Zinc900,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Zinc700),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSnooze(15) }
                    ) {
                        Text(
                            text = "⏱️ Snooze +15m",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }

                    Surface(
                        color = Zinc900,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Zinc700),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSnooze(30) }
                    ) {
                        Text(
                            text = "⏱️ Snooze +30m",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                }
            }
        }
    }
}
