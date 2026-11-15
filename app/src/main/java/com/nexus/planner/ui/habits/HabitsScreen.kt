package com.nexus.planner.ui.habits

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.planner.data.remote.ServerHabitDto
import com.nexus.planner.ui.theme.*

@Composable
fun HabitsScreen(
    habits: List<ServerHabitDto>,
    isLoading: Boolean = false,
    onToggleHabit: (Long) -> Unit,
    onLogMeasurableProgress: (Long, Double) -> Unit,
    onCreateHabit: (
        title: String,
        category: String,
        habitType: String,
        targetValue: Double,
        targetUnit: String,
        activeDays: String,
        reminderTime: String?
    ) -> Unit,
    onDeleteHabit: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var showUpsertSheet by remember { mutableStateOf(false) }
    var editingHabit by remember { mutableStateOf<ServerHabitDto?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Black)
            .padding(horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Header Row: HABITS + Active count badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "HABITS",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = Color.White
                )

                Surface(
                    color = Color(0xFF271B04),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF78350F))
                ) {
                    Text(
                        text = "🔥 ${habits.size} ACTIVE",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFBBF24),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            // Quick Create Habit Banner / Button
            Surface(
                color = Zinc950,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Zinc800),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        editingHabit = null
                        showUpsertSheet = true
                    }
                    .padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("✨", fontSize = 14.sp)
                        Column {
                            Text(
                                text = "NEW HABIT COMMITMENT",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Add yes/no daily discipline or measurable target",
                                fontSize = 10.sp,
                                color = Zinc400
                            )
                        }
                    }

                    Text(
                        text = "+ ADD",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Emerald400
                    )
                }
            }

            // Habits List
            if (habits.isEmpty() && !isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "NO HABITS CONFIGURED",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Zinc500
                        )
                        Text(
                            text = "Tap '+ ADD' or ask Nexus in chat to set habits",
                            fontSize = 11.sp,
                            color = Zinc600
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 40.dp)
                ) {
                    items(habits, key = { it.id ?: it.title }) { habit ->
                        HabitCardItem(
                            habit = habit,
                            onToggle = { habit.id?.let { onToggleHabit(it) } },
                            onLogProgress = { amount -> habit.id?.let { onLogMeasurableProgress(it, amount) } },
                            onClick = {
                                editingHabit = habit
                                showUpsertSheet = true
                            }
                        )
                    }
                }
            }
        }

        // Upsert BottomSheet
        if (showUpsertSheet) {
            HabitUpsertSheet(
                habit = editingHabit,
                onDismissRequest = {
                    showUpsertSheet = false
                    editingHabit = null
                },
                onSaveHabit = onCreateHabit,
                onDeleteHabit = onDeleteHabit
            )
        }
    }
}

@Composable
fun HabitCardItem(
    habit: ServerHabitDto,
    onToggle: () -> Unit,
    onLogProgress: (Double) -> Unit,
    onClick: () -> Unit
) {
    val isMeasurable = habit.habit_type == "MEASURABLE" && habit.target_value > 0
    val progressFraction = if (isMeasurable) {
        (habit.today_logged_value / habit.target_value).toFloat().coerceIn(0f, 1f)
    } else {
        if (habit.is_completed_today) 1f else 0f
    }
    val isCompleted = progressFraction >= 1f || habit.is_completed_today

    Surface(
        color = Zinc950,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (isCompleted) Emerald400.copy(alpha = 0.5f) else Zinc800),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Row: Title, Category & Streak
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = habit.title,
                            fontFamily = FontFamily.Default,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        if (habit.reminder_time != null) {
                            Text("🔔", fontSize = 11.sp)
                        }
                    }
                    Text(
                        text = if (isMeasurable) {
                            "Target: ${habit.target_value.toInt()} ${habit.target_unit} // ${habit.category}"
                        } else {
                            "Daily discipline // ${habit.category}"
                        },
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = Zinc400,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Surface(
                    color = Color(0xFF271B04),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF78350F))
                ) {
                    Text(
                        text = "🔥 ${habit.streak}D",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFBBF24),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Middle: If Measurable, show Progress Bar & Quick Log Increments
            if (isMeasurable) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Today's Progress",
                            fontSize = 11.sp,
                            color = Zinc400
                        )
                        Text(
                            text = "${habit.today_logged_value.toInt()} / ${habit.target_value.toInt()} ${habit.target_unit} (${(progressFraction * 100).toInt()}%)",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isCompleted) Emerald400 else Color.White
                        )
                    }

                    // Progress Track
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Zinc900)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progressFraction)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (isCompleted) Emerald400 else Color(0xFF60A5FA))
                        )
                    }

                    // Quick Log Buttons
                    val inc1 = if (habit.target_unit == "ml") 250.0 else 1.0
                    val inc2 = if (habit.target_unit == "ml") 500.0 else 5.0
                    val inc3 = if (habit.target_unit == "ml") 1000.0 else 10.0

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(inc1, inc2, inc3).forEach { amt ->
                            Surface(
                                color = Zinc900,
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, Zinc800),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onLogProgress(amt) }
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 5.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "+${amt.toInt()}${habit.target_unit}",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Zinc300
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // If Binary (Yes/No), show check toggle button
                Surface(
                    color = if (isCompleted) Emerald400.copy(alpha = 0.15f) else Zinc900,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (isCompleted) Emerald400 else Zinc800),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggle() }
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isCompleted) "COMPLETED TODAY" else "MARK AS COMPLETED TODAY",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isCompleted) Emerald400 else Zinc400
                        )
                        Text(
                            text = if (isCompleted) "✓" else "+",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isCompleted) Emerald400 else Zinc500
                        )
                    }
                }
            }

            // Bottom: 7-Day Week Bubbles Row (M, T, W, T, F, S, S)
            val weekDays = listOf("M", "T", "W", "T", "F", "S", "S")
            val history = if (habit.week_history.size == 7) habit.week_history else listOf(true, true, true, true, false, false, isCompleted)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                weekDays.forEachIndexed { idx, day ->
                    val done = history.getOrElse(idx) { false }
                    Surface(
                        color = if (done) Emerald400.copy(alpha = 0.2f) else Zinc900,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, if (done) Emerald400 else Zinc800),
                        modifier = Modifier
                            .size(width = 38.dp, height = 44.dp)
                            .clickable {
                                if (idx == 6) {
                                    if (isMeasurable) onLogProgress(if (habit.target_unit == "ml") 500.0 else 1.0) else onToggle()
                                }
                            }
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = day,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                color = Zinc500
                            )
                            Text(
                                text = if (done) "✓" else "-",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (done) Emerald400 else Zinc700
                            )
                        }
                    }
                }
            }
        }
    }
}
