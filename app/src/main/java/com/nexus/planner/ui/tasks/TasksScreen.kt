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
import androidx.compose.runtime.*
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
    todayTasks: List<ServerScheduleBlockDto>,
    tomorrowTasks: List<ServerScheduleBlockDto>,
    isAutoPlanEnabled: Boolean,
    onToggleTask: (Long, Boolean) -> Unit,
    onToggleAutoPlan: () -> Unit,
    onTriggerAlarm: (ServerScheduleBlockDto) -> Unit,
    onQuickAddTask: (String, Boolean) -> Unit,
    onDeleteTask: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf("today") }
    var editingTask by remember { mutableStateOf<ServerScheduleBlockDto?>(null) }
    var quickInputText by remember { mutableStateOf("") }

    val currentTasks = if (selectedTab == "today") todayTasks else tomorrowTasks
    val isTomorrow = selectedTab == "tomorrow"

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Black)
            .padding(horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // Header Bar: Simply "TASKS" + Date Switcher (TODAY vs TOMORROW)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TASKS",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = Color.White
                )

                // Date Switcher Tabs
                Surface(
                    color = Zinc900,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Zinc800)
                ) {
                    Row(modifier = Modifier.padding(2.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selectedTab == "today") Color.White else Color.Transparent)
                                .clickable { selectedTab = "today" }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "TODAY",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedTab == "today") Color.Black else Zinc400
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selectedTab == "tomorrow") Color.White else Color.Transparent)
                                .clickable { selectedTab = "tomorrow" }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "TOMORROW",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedTab == "tomorrow") Color.Black else Zinc400
                            )
                        }
                    }
                }
            }

            // Auto-Plan Mode Switch Bar
            Surface(
                color = Zinc950,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Zinc800),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (isAutoPlanEnabled) "⚡ Auto-Plan ON" else "⚡ Auto-Plan OFF",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isAutoPlanEnabled) Emerald400 else Zinc500
                        )
                        Text(
                            text = if (isAutoPlanEnabled) "Agent optimizing order" else "Static manual tasks",
                            fontSize = 11.sp,
                            color = Zinc400
                        )
                    }

                    Switch(
                        checked = isAutoPlanEnabled,
                        onCheckedChange = { onToggleAutoPlan() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = Emerald400,
                            uncheckedThumbColor = Zinc500,
                            uncheckedTrackColor = Zinc800
                        )
                    )
                }
            }

            // Quick Add Input
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = quickInputText,
                    onValueChange = { quickInputText = it },
                    placeholder = { Text("+ Add a task (e.g. 'Gym workout at 5 PM')...", fontSize = 12.sp, color = Zinc500) },
                    modifier = Modifier.weight(1f),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Zinc700,
                        unfocusedBorderColor = Zinc800,
                        focusedContainerColor = Zinc950,
                        unfocusedContainerColor = Zinc950
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                if (quickInputText.isNotBlank()) {
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.clickable {
                            val t = quickInputText
                            quickInputText = ""
                            onQuickAddTask(t, isTomorrow)
                        }
                    ) {
                        Text(
                            text = "ADD",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp)
                        )
                    }
                }
            }

            // Tasks List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 40.dp)
            ) {
                if (currentTasks.isEmpty()) {
                    item {
                        Surface(
                            color = Zinc950,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Zinc800),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = if (isTomorrow) "🌙" else "🎯",
                                    fontSize = 28.sp
                                )
                                Text(
                                    text = if (isTomorrow) "Tomorrow is open" else "No tasks scheduled for today",
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "Add a task above or ask Nexus in chat to plan your day.",
                                    fontSize = 12.sp,
                                    color = Zinc500
                                )
                            }
                        }
                    }
                } else {
                    items(currentTasks, key = { it.id ?: it.title.hashCode().toLong() }) { task ->
                        TaskListItem(
                            task = task,
                            onToggleDone = { task.id?.let { onToggleTask(it, isTomorrow) } },
                            onClick = { editingTask = task }
                        )
                    }
                }
            }
        }

        // Task Detail Sheet (Customizer for Title, Time, Alarm, Delete)
        editingTask?.let { task ->
            TaskDetailCustomizerSheet(
                task = task,
                onDismiss = { editingTask = null },
                onTriggerAlarm = {
                    editingTask = null
                    onTriggerAlarm(task)
                },
                onDelete = {
                    editingTask = null
                    task.id?.let { onDeleteTask(it) }
                }
            )
        }
    }
}

fun getContextualTaskIcon(title: String, blockType: String): String {
    val t = title.lowercase()
    val b = blockType.uppercase()
    return when {
        t.contains("gym") || t.contains("workout") || t.contains("exercise") || t.contains("pushup") || t.contains("lift") || b.contains("WORKOUT") -> "🏋️"
        t.contains("run") || t.contains("jog") || t.contains("walk") || t.contains("cardio") -> "🏃"
        t.contains("laptop") || t.contains("code") || t.contains("dev") || t.contains("saas") || t.contains("pr") || t.contains("debug") || t.contains("build") || t.contains("project") || t.contains("study") || t.contains("work") || b.contains("FOCUS") || b.contains("SAAS") -> "💻"
        t.contains("book") || t.contains("read") || t.contains("research") || t.contains("learn") -> "📚"
        t.contains("call") || t.contains("meet") || t.contains("sync") || t.contains("interview") -> "📞"
        t.contains("coffee") || t.contains("tea") || t.contains("chai") -> "☕"
        t.contains("eat") || t.contains("lunch") || t.contains("dinner") || t.contains("breakfast") || t.contains("meal") || t.contains("food") -> "🥗"
        t.contains("sleep") || t.contains("bed") || b.contains("SLEEP") -> "🌙"
        t.contains("rest") || t.contains("nap") || t.contains("nsdr") || t.contains("meditat") || t.contains("recharge") || b.contains("TRANSITION") || b.contains("REST") || b.contains("NAP") -> "🔋"
        t.contains("clean") || t.contains("chore") || t.contains("laundry") || t.contains("grocer") -> "🧹"
        else -> "🎯"
    }
}

@Composable
fun TaskListItem(
    task: ServerScheduleBlockDto,
    onToggleDone: () -> Unit,
    onClick: () -> Unit
) {
    val bType = task.block_type.uppercase()
    val isGym = task.title.contains("gym", ignoreCase = true) || bType.contains("WORKOUT")
    val isWork = task.title.contains("laptop", ignoreCase = true) || task.title.contains("pr", ignoreCase = true) || bType.contains("FOCUS") || bType.contains("SAAS")
    val isRest = bType.contains("TRANSITION") || bType.contains("REST") || bType.contains("NAP")
    val isSleep = bType.contains("SLEEP")

    val contextualIcon = getContextualTaskIcon(task.title, task.block_type)

    val edgeColor = when {
        task.is_completed -> Zinc700
        isGym -> Color(0xFFFBBF24)
        isWork -> Color.White
        isRest -> Color(0xFF60A5FA)
        isSleep -> Zinc800
        else -> Emerald400
    }

    val isCarriedOver = task.status == "CARRIED_OVER"

    Surface(
        color = if (task.is_completed) Zinc950.copy(alpha = 0.5f) else Zinc950,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (isCarriedOver) Color(0xFF78350F) else Zinc900),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
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
                // Colored Edge Bar
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(38.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(edgeColor)
                )

                // Checkbox Circle
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .border(1.dp, if (task.is_completed) Emerald400 else Zinc700, CircleShape)
                        .background(if (task.is_completed) Emerald400.copy(alpha = 0.2f) else Color.Transparent)
                        .clickable { onToggleDone() },
                    contentAlignment = Alignment.Center
                ) {
                    if (task.is_completed) {
                        Text(text = "✓", color = Emerald400, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Title and Time Row
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = contextualIcon, fontSize = 14.sp)
                        Text(
                            text = task.title,
                            fontSize = 14.sp,
                            fontWeight = if (task.is_completed) FontWeight.Normal else FontWeight.Medium,
                            color = if (task.is_completed) Zinc500 else Color.White,
                            textDecoration = if (task.is_completed) TextDecoration.LineThrough else TextDecoration.None
                        )
                        if (isCarriedOver) {
                            Surface(
                                color = Color(0xFF451A03),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "🔄 Carried over",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    color = Color(0xFFFCD34D),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${task.start_time} – ${task.end_time}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Zinc400
                        )
                        // Alarm Icon shown only if alarm is set
                        Text(text = "⏰", fontSize = 11.sp)
                    }
                }
            }

            Text(text = "›", fontSize = 16.sp, color = Zinc600)
        }
    }
}

@Composable
fun TaskDetailCustomizerSheet(
    task: ServerScheduleBlockDto,
    onDismiss: () -> Unit,
    onTriggerAlarm: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        color = Color.Black.copy(alpha = 0.75f),
        modifier = Modifier
            .fillMaxSize()
            .clickable { onDismiss() }
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                color = Zinc950,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                border = BorderStroke(1.dp, Zinc800),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = false) {}
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TASK DETAILS",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Zinc400
                        )
                        Text(
                            text = "CLOSE",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Zinc500,
                            modifier = Modifier.clickable { onDismiss() }
                        )
                    }

                    // Task Title with Contextual Icon
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = getContextualTaskIcon(task.title, task.block_type),
                            fontSize = 20.sp
                        )
                        Text(
                            text = task.title,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }

                    // Time Info
                    Surface(
                        color = Zinc900,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SCHEDULED TIME",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = Zinc400
                            )
                            Text(
                                text = "${task.start_time} – ${task.end_time} (${task.duration_minutes}m)",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    // Test Full-Screen Alarm Button
                    Surface(
                        color = Zinc900,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF7F1D1D)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTriggerAlarm() }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("🚨", fontSize = 18.sp)
                            Column {
                                Text(
                                    text = "TEST FULL-SCREEN ALARM",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFCA5A5)
                                )
                                Text(
                                    text = "Preview incoming alarm HUD with voice mic",
                                    fontSize = 11.sp,
                                    color = Zinc400
                                )
                            }
                        }
                    }

                    // Delete Action
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = "DELETE TASK",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEF4444),
                            modifier = Modifier
                                .clickable { onDelete() }
                                .padding(vertical = 8.dp, horizontal = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
