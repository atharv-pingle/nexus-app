package com.nexus.planner.ui.tasks

import androidx.compose.animation.*
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
    isLoading: Boolean = false,
    onToggleTask: (Long, Boolean) -> Unit,
    onToggleAutoPlan: () -> Unit,
    onTriggerAlarm: (ServerScheduleBlockDto) -> Unit,
    onQuickAddTask: (String, Boolean) -> Unit,
    onDeleteTask: (Long) -> Unit,
    onUpdateTask: (Long, String, String, Int, Boolean) -> Unit,
    onCreateFullTask: ((String, String, Int, Boolean, String, String?, List<String>) -> Unit)? = null,
    onToggleSubTask: ((Long, Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf("today") }
    var editingTask by remember { mutableStateOf<ServerScheduleBlockDto?>(null) }
    var showCreateSheet by remember { mutableStateOf(false) }
    var selectedCategoryFilter by remember { mutableStateOf("ALL") }
    var quickInputText by remember { mutableStateOf("") }

    val rawTasks = if (selectedTab == "today") todayTasks else tomorrowTasks
    val currentTasks = if (selectedCategoryFilter == "ALL") {
        rawTasks
    } else {
        rawTasks.filter { it.block_type.equals(selectedCategoryFilter, ignoreCase = true) }
    }
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
                            text = "⚡ AUTO-PLAN",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isAutoPlanEnabled) Emerald400 else Zinc500
                        )
                        Text(
                            text = if (isAutoPlanEnabled) "Dynamic schedule" else "Manual schedule",
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

            // Category Filter Chips Row (Grit feature)
            val categoryChips = listOf("ALL", "FOCUS", "DEEP WORK", "SIDE PROJECT", "WORKOUT", "ADMIN")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                categoryChips.take(5).forEach { cat ->
                    val isSelected = selectedCategoryFilter == cat
                    Surface(
                        color = if (isSelected) Color.White else Zinc950,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (isSelected) Color.White else Zinc800),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedCategoryFilter = cat }
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = cat,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.Black else Zinc400,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Animated Processing Pop-up Banner (Auto-Plan / Rebalance in progress)
            AnimatedVisibility(
                visible = isLoading,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    color = Color(0xFF18181B),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Emerald400.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(13.dp),
                            strokeWidth = 2.dp,
                            color = Emerald400
                        )
                        Text(
                            text = "⚡ Auto-Plan is analyzing & rebalancing...",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Emerald400
                        )
                    }
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
                } else {
                    Surface(
                        color = Zinc900,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Zinc800),
                        modifier = Modifier.clickable { showCreateSheet = true }
                    ) {
                        Text(
                            text = "⚙ DETAILS",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Zinc300,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp)
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
                            isAutoPlanEnabled = isAutoPlanEnabled,
                            onToggleDone = { task.id?.let { onToggleTask(it, isTomorrow) } },
                            onClick = { editingTask = task }
                        )
                    }
                }
            }
        }

        // Create Task Sheet (Full Details, Alarms & Subtasks)
        if (showCreateSheet) {
            TaskUpsertSheet(
                initialIsTomorrow = isTomorrow,
                onDismissRequest = { showCreateSheet = false },
                onSaveTask = { title, startTime, duration, isTomorrowTarget, category, reminderTime, subtasks ->
                    showCreateSheet = false
                    if (onCreateFullTask != null) {
                        onCreateFullTask(title, startTime, duration, isTomorrowTarget, category, reminderTime, subtasks)
                    } else {
                        onQuickAddTask(title, isTomorrowTarget)
                    }
                }
            )
        }

        // Edit Task Sheet
        editingTask?.let { task ->
            TaskUpsertSheet(
                task = task,
                initialIsTomorrow = isTomorrow,
                onDismissRequest = { editingTask = null },
                onTriggerAlarm = {
                    editingTask = null
                    onTriggerAlarm(task)
                },
                onDeleteTask = { id ->
                    editingTask = null
                    onDeleteTask(id)
                },
                onSaveTask = { title, startTime, duration, isTomorrowTarget, category, reminderTime, subtasks ->
                    editingTask = null
                    task.id?.let { id ->
                        onUpdateTask(id, title, startTime, duration, isTomorrowTarget)
                    }
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
    isAutoPlanEnabled: Boolean,
    onToggleDone: () -> Unit,
    onClick: () -> Unit,
    onToggleSubTask: ((Int) -> Unit)? = null
) {
    var isSubtasksExpanded by remember { mutableStateOf(false) }
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                            val isAiManaged = task.status == "AI_SCHEDULED" || task.status == "CARRIED_OVER"
                            if (isAiManaged && !task.is_completed) {
                                Text(text = "✨", fontSize = 11.sp)
                            }
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

                            // Category Tag
                            Surface(
                                color = Zinc900,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = task.block_type,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 8.5.sp,
                                    color = Zinc400,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }

                            if (task.reminder_time != null) {
                                Text(
                                    text = "🔔 ${task.reminder_time}",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = Emerald400
                                )
                            }

                            if (task.subtasks.isNotEmpty()) {
                                val completedCount = task.subtasks.count { it.is_completed }
                                Surface(
                                    color = Color(0xFF1E293B),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.clickable { isSubtasksExpanded = !isSubtasksExpanded }
                                ) {
                                    Text(
                                        text = "▾ $completedCount/${task.subtasks.size} subtasks",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 8.5.sp,
                                        color = Color(0xFF93C5FD),
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Text(text = "›", fontSize = 16.sp, color = Zinc600)
            }

            // Expandable Subtasks Accordion (Grit feature)
            if (task.subtasks.isNotEmpty() && isSubtasksExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 38.dp, top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    task.subtasks.forEachIndexed { sIdx, sub ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.clickable { onToggleSubTask?.invoke(sIdx) }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(15.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .border(1.dp, if (sub.is_completed) Emerald400 else Zinc600, RoundedCornerShape(3.dp))
                                    .background(if (sub.is_completed) Emerald400 else Color.Transparent),
                                contentAlignment = Alignment.Center
                            ) {
                                if (sub.is_completed) {
                                    Text(text = "✓", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Text(
                                text = sub.title,
                                fontSize = 12.sp,
                                color = if (sub.is_completed) Zinc500 else Zinc300,
                                textDecoration = if (sub.is_completed) TextDecoration.LineThrough else TextDecoration.None
                            )
                        }
                    }
                }
            }
        }
    }
}
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
    onDelete: () -> Unit,
    onSave: (Long, String, String, Int, Boolean) -> Unit
) {
    var editTitle by remember { mutableStateOf(task.title) }
    var editStartTime by remember { mutableStateOf(task.start_time) }
    var editDuration by remember { mutableStateOf(task.duration_minutes) }
    var editIsTomorrow by remember {
        val tomorrowDate = java.time.LocalDate.now().plusDays(1).toString()
        mutableStateOf(task.scheduled_date == tomorrowDate)
    }

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
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = getContextualTaskIcon(editTitle, task.block_type), fontSize = 18.sp)
                            Text(
                                text = "EDIT TASK",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Zinc400
                            )
                        }
                        Text(
                            text = "CLOSE",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Zinc500,
                            modifier = Modifier.clickable { onDismiss() }
                        )
                    }

                    // Editable Title
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White, fontSize = 14.5.sp),
                        label = { Text("Task Title", fontSize = 10.sp, color = Zinc500) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Zinc600,
                            unfocusedBorderColor = Zinc800,
                            focusedContainerColor = Zinc900,
                            unfocusedContainerColor = Zinc900
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    // Scheduled Day Switcher (TODAY vs TOMORROW)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = if (!editIsTomorrow) Color.White else Zinc900,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, if (!editIsTomorrow) Color.White else Zinc800),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { editIsTomorrow = false }
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "TODAY",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (!editIsTomorrow) Color.Black else Zinc400
                                )
                            }
                        }

                        Surface(
                            color = if (editIsTomorrow) Color.White else Zinc900,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, if (editIsTomorrow) Color.White else Zinc800),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { editIsTomorrow = true }
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "TOMORROW",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (editIsTomorrow) Color.Black else Zinc400
                                )
                            }
                        }
                    }

                    // Start Time & Duration Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = editStartTime,
                            onValueChange = { editStartTime = it },
                            label = { Text("Start Time", fontSize = 10.sp, color = Zinc500) },
                            modifier = Modifier.weight(1f),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White, fontFamily = FontFamily.Monospace),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Zinc600,
                                unfocusedBorderColor = Zinc800,
                                focusedContainerColor = Zinc900,
                                unfocusedContainerColor = Zinc900
                            ),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = editDuration.toString(),
                            onValueChange = { editDuration = it.toIntOrNull() ?: editDuration },
                            label = { Text("Duration (m)", fontSize = 10.sp, color = Zinc500) },
                            modifier = Modifier.weight(1f),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White, fontFamily = FontFamily.Monospace),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Zinc600,
                                unfocusedBorderColor = Zinc800,
                                focusedContainerColor = Zinc900,
                                unfocusedContainerColor = Zinc900
                            ),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }

                    // Quick Duration Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(15, 30, 45, 60, 90).forEach { mins ->
                            Surface(
                                color = if (editDuration == mins) Emerald400.copy(alpha = 0.2f) else Zinc900,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, if (editDuration == mins) Emerald400 else Zinc800),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { editDuration = mins }
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${mins}m",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (editDuration == mins) Emerald400 else Zinc400
                                    )
                                }
                            }
                        }
                    }

                    // Save Changes Button
                    Button(
                        onClick = {
                            task.id?.let { id ->
                                onSave(id, editTitle, editStartTime, editDuration, editIsTomorrow)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Text(
                            text = "SAVE CHANGES",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }

                    // Bottom Row: Test Alarm & Delete
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🚨 TEST ALARM",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFCA5A5),
                            modifier = Modifier
                                .clickable { onTriggerAlarm() }
                                .padding(vertical = 6.dp, horizontal = 4.dp)
                        )

                        Text(
                            text = "DELETE TASK",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEF4444),
                            modifier = Modifier
                                .clickable { onDelete() }
                                .padding(vertical = 6.dp, horizontal = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
