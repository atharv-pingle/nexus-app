package com.nexus.planner.ui.habits

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.nexus.planner.data.remote.ServerHabitDto
import com.nexus.planner.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitUpsertSheet(
    habit: ServerHabitDto? = null,
    onDismissRequest: () -> Unit,
    onSaveHabit: (
        title: String,
        category: String,
        habitType: String,
        targetValue: Double,
        targetUnit: String,
        activeDays: String,
        reminderTime: String?
    ) -> Unit,
    onDeleteHabit: ((Long) -> Unit)? = null
) {
    val isEdit = habit != null
    var title by remember { mutableStateOf(habit?.title ?: "") }
    var selectedCategory by remember { mutableStateOf(habit?.category ?: "FOCUS") }
    var habitType by remember { mutableStateOf(habit?.habit_type ?: "YES_NO") } // YES_NO or MEASURABLE
    var targetValueStr by remember { mutableStateOf(if ((habit?.target_value ?: 0.0) > 0) habit?.target_value.toString() else "3000") }
    var targetUnit by remember { mutableStateOf(habit?.target_unit ?: "ml") }
    var hasReminder by remember { mutableStateOf(habit?.reminder_time != null) }
    var reminderTime by remember { mutableStateOf(habit?.reminder_time ?: "08:00") }

    // Active days of week: Mon, Tue, Wed, Thu, Fri, Sat, Sun
    val allDays = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
    var selectedDays by remember {
        val initial = habit?.active_days?.split(",")?.map { it.trim().uppercase() }?.toSet()
            ?: allDays.toSet()
        mutableStateOf(initial)
    }

    val categories = listOf("FOCUS", "HEALTH", "DISCIPLINE", "MINDFULNESS", "FITNESS")

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
                            text = if (isEdit) "EDIT HABIT" else "CREATE HABIT",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = Color.White
                        )
                        Text(
                            text = if (isEdit) "Adjust targets, frequency & alarms" else "Configure daily habit & streak tracking",
                            fontSize = 11.sp,
                            color = Zinc400
                        )
                    }

                    if (isEdit && habit?.id != null && onDeleteHabit != null) {
                        Text(
                            text = "DELETE",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEF4444),
                            modifier = Modifier
                                .clickable {
                                    onDeleteHabit(habit.id)
                                    onDismissRequest()
                                }
                                .padding(4.dp)
                        )
                    }
                }
            }

            // Habit Type Segmented Selector: YES/NO vs MEASURABLE
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "HABIT TYPE",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Zinc400
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = if (habitType == "YES_NO") Color.White else Zinc900,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, if (habitType == "YES_NO") Color.White else Zinc800),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { habitType = "YES_NO" }
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "YES / NO",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (habitType == "YES_NO") Color.Black else Zinc300
                                )
                                Text(
                                    text = "e.g. Gym, Cold shower",
                                    fontSize = 9.sp,
                                    color = if (habitType == "YES_NO") Zinc700 else Zinc500
                                )
                            }
                        }

                        Surface(
                            color = if (habitType == "MEASURABLE") Color.White else Zinc900,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, if (habitType == "MEASURABLE") Color.White else Zinc800),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { habitType = "MEASURABLE" }
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "MEASURABLE",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (habitType == "MEASURABLE") Color.Black else Zinc300
                                )
                                Text(
                                    text = "e.g. 3000 ml, 20 pages",
                                    fontSize = 9.sp,
                                    color = if (habitType == "MEASURABLE") Zinc700 else Zinc500
                                )
                            }
                        }
                    }
                }
            }

            // Title Field
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "HABIT NAME",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Zinc400
                    )
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = { Text("e.g. Daily Hydration Target", fontSize = 13.sp, color = Zinc500) },
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

            // Measurable Target & Unit (Only shown if MEASURABLE)
            if (habitType == "MEASURABLE") {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = targetValueStr,
                            onValueChange = { targetValueStr = it },
                            label = { Text("Target Value", fontSize = 10.sp, color = Zinc500) },
                            modifier = Modifier.weight(1.2f),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White, fontFamily = FontFamily.Monospace),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Emerald400,
                                unfocusedBorderColor = Zinc800,
                                focusedContainerColor = Zinc950,
                                unfocusedContainerColor = Zinc950
                            ),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = targetUnit,
                            onValueChange = { targetUnit = it },
                            label = { Text("Unit (ml, km, pages)", fontSize = 10.sp, color = Zinc500) },
                            modifier = Modifier.weight(1f),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White, fontFamily = FontFamily.Monospace),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Emerald400,
                                unfocusedBorderColor = Zinc800,
                                focusedContainerColor = Zinc950,
                                unfocusedContainerColor = Zinc950
                            ),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }
                }
            }

            // Active Days of Week (M, T, W, T, F, S, S)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "ACTIVE DAYS",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Zinc400
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        allDays.forEach { day ->
                            val isSel = selectedDays.contains(day)
                            Surface(
                                color = if (isSel) Emerald400 else Zinc900,
                                shape = CircleShape,
                                border = BorderStroke(1.dp, if (isSel) Emerald400 else Zinc800),
                                modifier = Modifier
                                    .size(42.dp)
                                    .clickable {
                                        selectedDays = if (isSel) {
                                            if (selectedDays.size > 1) selectedDays - day else selectedDays
                                        } else {
                                            selectedDays + day
                                        }
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = day.take(1),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSel) Color.Black else Zinc400
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Daily Alarm / Reminder Toggle
            item {
                Surface(
                    color = Zinc950,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (hasReminder) Emerald400.copy(alpha = 0.5f) else Zinc800),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("🔔", fontSize = 14.sp)
                            Column {
                                Text(
                                    text = "DAILY HABIT ALARM",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (hasReminder) Emerald400 else Zinc400
                                )
                                Text(
                                    text = if (hasReminder) "Rings at $reminderTime on active days" else "No reminder scheduled",
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
                }
            }

            // Save Button
            item {
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            val activeDaysStr = allDays.filter { selectedDays.contains(it) }.joinToString(",")
                            val targetVal = targetValueStr.toDoubleOrNull() ?: 0.0
                            onSaveHabit(
                                title.trim(),
                                selectedCategory,
                                habitType,
                                if (habitType == "MEASURABLE") targetVal else 0.0,
                                if (habitType == "MEASURABLE") targetUnit.trim() else "",
                                activeDaysStr,
                                if (hasReminder) reminderTime.trim() else null
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
                        text = if (isEdit) "SAVE HABIT" else "CREATE HABIT",
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
