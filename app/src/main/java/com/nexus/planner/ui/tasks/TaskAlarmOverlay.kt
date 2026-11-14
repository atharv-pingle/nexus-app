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
            // Top Status Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444))
                    )
                    Text(
                        text = "NEXUS // TASK ALARM",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFCA5A5)
                    )
                }

                Surface(
                    color = Zinc900,
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, Zinc800)
                ) {
                    Text(
                        text = "PRIORITY ALERT",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = Zinc400,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
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

                // Dedicated Mic on Alarm Screen
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFDC2626))
                            .clickable { onStartVoiceRecognition() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Task Voice Instruction",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Text(
                        text = "🎙️ Tap to talk to this task",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = Zinc400
                    )

                    // Quick Spoken Suggestions
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Surface(
                            color = Zinc900,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Zinc800),
                            modifier = Modifier.clickable { onVoiceInstruction("Delay 20 mins") }
                        ) {
                            Text(
                                text = "⏱️ Delay 20m",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = Zinc300,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }

                        Surface(
                            color = Zinc900,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Zinc800),
                            modifier = Modifier.clickable { onVoiceInstruction("Move to tomorrow") }
                        ) {
                            Text(
                                text = "🔄 Move to tomorrow",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = Zinc300,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
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
