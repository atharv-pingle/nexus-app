package com.nexus.planner.ui.tasks

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.planner.data.remote.ServerScheduleBlockDto
import com.nexus.planner.data.remote.ServerSubTaskDto
import com.nexus.planner.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskUpsertSheet(
    task: ServerScheduleBlockDto? = null,
    initialIsTomorrow: Boolean = false,
    onDismissRequest: () -> Unit,
    onSaveTask: (
        title: String,
        startTime: String,
        durationMinutes: Int,
        isTomorrow: Boolean,
        category: String,
        reminderTime: String?,
        subtasks: List<String>
    ) -> Unit,
    onDeleteTask: ((Long) -> Unit)? = null,
    onTriggerAlarm: (() -> Unit)? = null
) {
    val isEdit = task != null
    var title by remember { mutableStateOf(task?.title ?: "") }
    var startTime by remember { mutableStateOf(task?.start_time ?: "10:00") }
    var durationMinutes by remember { mutableStateOf(task?.duration_minutes ?: 45) }
    var isTomorrow by remember { mutableStateOf(if (isEdit) (task?.scheduled_date?.contains("tomorrow") == true) else initialIsTomorrow) }
    var selectedCategory by remember { mutableStateOf(task?.block_type ?: "FOCUS") }
    var hasReminder by remember { mutableStateOf(task?.reminder_time != null) }
    var reminderTime by remember { mutableStateOf(task?.reminder_time ?: startTime) }

    // Subtasks builder list
    var subtasksList by remember {
        mutableStateOf(task?.subtasks?.map { it.title } ?: emptyList())
    }
    var newSubtaskInput by remember { mutableStateOf("") }

    val categories = listOf("FOCUS", "DEEP WORK", "SIDE PROJECT", "WORKOUT", "ADMIN")

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = Color(0xFF09090B),
        contentColor = Color.White,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .background(Zinc700, RoundedCornerShape(2.dp))
            )
        }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isEdit) "EDIT TASK" else "CREATE TASK",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = Color.White
                        )
                        Text(
                            text = if (isEdit) "Modify details, subtasks & alarms" else "Add task with subtasks & exact alarm",
                            fontSize = 11.sp,
                            color = Zinc400
                        )
                    }

                    if (isEdit && task?.id != null && onDeleteTask != null) {
                        Text(
                            text = "DELETE",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEF4444),
                            modifier = Modifier
                                .clickable {
                                    onDeleteTask(task.id)
                                    onDismissRequest()
                                }
                                .padding(4.dp)
                        )
                    }
                }
            }

            // Category Chips Carousel
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "CATEGORY",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Zinc400
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categories.forEach { cat ->
                            val isSel = selectedCategory.equals(cat, ignoreCase = true)
                            Surface(
                                color = if (isSel) Color.White else Zinc900,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, if (isSel) Color.White else Zinc800),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedCategory = cat }
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 7.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = cat,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSel) Color.Black else Zinc400,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Task Title Input
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "TASK TITLE",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Zinc400
                    )
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = { Text("What needs to be done?", fontSize = 13.sp, color = Zinc500) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Zinc600,
                            unfocusedBorderColor = Zinc800,
                            focusedContainerColor = Zinc950,
                            unfocusedContainerColor = Zinc950
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
            }

            // Schedule Day Switcher: TODAY vs TOMORROW
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = if (!isTomorrow) Color.White else Zinc900,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, if (!isTomorrow) Color.White else Zinc800),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { isTomorrow = false }
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 9.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "TODAY",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (!isTomorrow) Color.Black else Zinc400
                            )
                        }
                    }

                    Surface(
                        color = if (isTomorrow) Color.White else Zinc900,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, if (isTomorrow) Color.White else Zinc800),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { isTomorrow = true }
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 9.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "TOMORROW",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isTomorrow) Color.Black else Zinc400
                            )
                        }
                    }
                }
            }

            // Start Time & Duration
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("Start Time (HH:mm)", fontSize = 10.sp, color = Zinc500) },
                        modifier = Modifier.weight(1f),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White, fontFamily = FontFamily.Monospace),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Zinc600,
                            unfocusedBorderColor = Zinc800,
                            focusedContainerColor = Zinc950,
                            unfocusedContainerColor = Zinc950
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = durationMinutes.toString(),
                        onValueChange = { durationMinutes = it.toIntOrNull() ?: durationMinutes },
                        label = { Text("Duration (mins)", fontSize = 10.sp, color = Zinc500) },
                        modifier = Modifier.weight(1f),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White, fontFamily = FontFamily.Monospace),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Zinc600,
                            unfocusedBorderColor = Zinc800,
                            focusedContainerColor = Zinc950,
                            unfocusedContainerColor = Zinc950
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
            }

            // Alarm / Reminder Toggle
            item {
                Surface(
                    color = Zinc950,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (hasReminder) Emerald400.copy(alpha = 0.5f) else Zinc800),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "🔔",
                                    fontSize = 14.sp
                                )
                                Column {
                                    Text(
                                        text = "EXACT SYSTEM ALARM",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (hasReminder) Emerald400 else Zinc400
                                    )
                                    Text(
                                        text = if (hasReminder) "Heads-up alert & vibration will ring" else "No reminder scheduled",
                                        fontSize = 10.sp,
                                        color = Zinc500
                                    )
                                }
                            }

                            Switch(
                                checked = hasReminder,
                                onCheckedChange = { hasReminder = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.Black,
                                    checkedTrackColor = Emerald400,
                                    uncheckedThumbColor = Zinc500,
                                    uncheckedTrackColor = Zinc800
                                )
                            )
                        }

                        if (hasReminder) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = reminderTime,
                                    onValueChange = { reminderTime = it },
                                    label = { Text("Alarm Time (HH:mm)", fontSize = 10.sp, color = Zinc500) },
                                    modifier = Modifier.weight(1f),
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = Emerald400, fontFamily = FontFamily.Monospace),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Emerald400,
                                        unfocusedBorderColor = Zinc700,
                                        focusedContainerColor = Color.Black,
                                        unfocusedContainerColor = Color.Black
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    singleLine = true
                                )
                                if (onTriggerAlarm != null) {
                                    Surface(
                                        color = Zinc900,
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, Zinc800),
                                        modifier = Modifier.clickable { onTriggerAlarm() }
                                    ) {
                                        Text(
                                            text = "TEST",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFCA5A5),
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Subtasks Builder Section
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "SUBTASKS (${subtasksList.size})",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Zinc400
                    )

                    // Existing subtasks
                    subtasksList.forEachIndexed { idx, sub ->
                        Surface(
                            color = Zinc950,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Zinc800),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "${idx + 1}.",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = Zinc500
                                    )
                                    Text(
                                        text = sub,
                                        fontSize = 12.sp,
                                        color = Color.White
                                    )
                                }
                                Text(
                                    text = "✕",
                                    color = Zinc500,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clickable {
                                            subtasksList = subtasksList.toMutableList().apply { removeAt(idx) }
                                        }
                                        .padding(4.dp)
                                )
                            }
                        }
                    }

                    // Add Subtask Input
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newSubtaskInput,
                            onValueChange = { newSubtaskInput = it },
                            placeholder = { Text("+ Add a subtask step...", fontSize = 11.sp, color = Zinc500) },
                            modifier = Modifier.weight(1f),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Zinc600,
                                unfocusedBorderColor = Zinc800,
                                focusedContainerColor = Zinc950,
                                unfocusedContainerColor = Zinc950
                            ),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )

                        if (newSubtaskInput.isNotBlank()) {
                            Surface(
                                color = Color.White,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.clickable {
                                    subtasksList = subtasksList + newSubtaskInput.trim()
                                    newSubtaskInput = ""
                                }
                            ) {
                                Text(
                                    text = "ADD",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Save / Confirm Button
            item {
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            onSaveTask(
                                title.trim(),
                                startTime.trim(),
                                durationMinutes,
                                isTomorrow,
                                selectedCategory,
                                if (hasReminder) reminderTime.trim() else null,
                                subtasksList
                            )
                            onDismissRequest()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(
                        text = if (isEdit) "SAVE CHANGES" else "CREATE TASK",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}
