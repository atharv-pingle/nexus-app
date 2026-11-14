package com.nexus.planner.ui.tasks

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.planner.data.remote.ServerScheduleBlockDto
import com.nexus.planner.ui.theme.*

@Composable
fun TasksScreen(
    tasks: List<ServerScheduleBlockDto>,
    onToggleTask: (Long) -> Unit,
    onShiftSchedule: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val completedCount = tasks.count { it.is_completed }
    val totalCount = tasks.size

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Black)
            .padding(horizontal = 16.dp)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(top = 14.dp, bottom = 40.dp)
        ) {
            // Header Bar
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("📋", fontSize = 14.sp)
                            Text(
                                text = "TODAY'S SCHEDULE",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = Color.White
                            )
                        }
                        Text(
                            text = "Continuous plan until 23:30 bedtime",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Zinc500,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    Surface(
                        color = Zinc950,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, if (completedCount > 0) Emerald400 else Zinc800)
                    ) {
                        Text(
                            text = "$completedCount of $totalCount Done",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (completedCount > 0) Emerald400 else Zinc400,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Quick Shift Bar
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Shift schedule:",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Zinc500
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            color = Zinc900,
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, Zinc800),
                            modifier = Modifier.clickable { onShiftSchedule(15) }
                        ) {
                            Text(
                                text = "⏱️ +15m",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = Zinc300,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Surface(
                            color = Zinc900,
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, Zinc800),
                            modifier = Modifier.clickable { onShiftSchedule(30) }
                        ) {
                            Text(
                                text = "⏱️ +30m",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = Zinc300,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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

            // Task List Items
            items(tasks, key = { it.id }) { task ->
                TaskCardItem(
                    task = task,
                    onToggle = { onToggleTask(task.id) }
                )
            }
        }
    }
}

@Composable
fun TaskCardItem(
    task: ServerScheduleBlockDto,
    onToggle: () -> Unit
) {
    val bType = task.block_type.uppercase()
    val edgeColor = when {
        task.is_completed -> Zinc700
        bType.contains("FOCUS") || bType.contains("SAAS") -> Color.White
        bType.contains("FITNESS") -> Emerald400
        bType.contains("TRANSITION") || bType.contains("REST") || bType.contains("NAP") -> Color(0xFF60A5FA)
        bType.contains("WORKOUT") -> Color(0xFFFBBF24)
        bType.contains("SLEEP") -> Zinc800
        else -> Zinc600
    }

    val badgeLabel = when {
        task.is_completed -> "DONE"
        bType.contains("FITNESS") -> "FITNESS"
        bType.contains("TRANSITION") || bType.contains("REST") -> "REST"
        bType.contains("WORKOUT") -> "WORKOUT"
        bType.contains("SLEEP") -> "BEDTIME"
        task.status == "ACTIVE" || task.status == "IN_PROGRESS" -> "ACTIVE"
        else -> "UPCOMING"
    }

    val badgeColor = when {
        task.is_completed -> Zinc500
        bType.contains("FITNESS") -> Emerald400
        bType.contains("TRANSITION") || bType.contains("REST") -> Color(0xFF60A5FA)
        bType.contains("WORKOUT") -> Color(0xFFFBBF24)
        bType.contains("SLEEP") -> Zinc600
        task.status == "ACTIVE" -> Emerald400
        else -> Zinc400
    }

    Surface(
        color = if (task.is_completed) Zinc950.copy(alpha = 0.5f) else Zinc950,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (task.status == "ACTIVE" && !task.is_completed) Zinc700 else Zinc900),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Edge Accent Bar
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(36.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(edgeColor)
                )

                // Status Checkmark / Circle
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .border(1.dp, if (task.is_completed) Emerald400 else Zinc700, CircleShape)
                        .background(if (task.is_completed) Emerald400.copy(alpha = 0.2f) else Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    if (task.is_completed) {
                        Text(
                            text = "✓",
                            color = Emerald400,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else if (bType.contains("SLEEP")) {
                        Text(
                            text = "🌙",
                            fontSize = 10.sp
                        )
                    }
                }

                // Title and Time Range
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = task.title,
                        fontSize = 13.sp,
                        fontWeight = if (task.is_completed) FontWeight.Normal else FontWeight.Medium,
                        color = if (task.is_completed) Zinc500 else Color.White,
                        textDecoration = if (task.is_completed) TextDecoration.LineThrough else TextDecoration.None
                    )
                    Text(
                        text = "${task.start_time} – ${task.end_time} (${task.duration_minutes}m)",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Zinc500
                    )
                }
            }

            // Right Type Badge
            Surface(
                color = Zinc900,
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(1.dp, Zinc800)
            ) {
                Text(
                    text = badgeLabel,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = badgeColor,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}
