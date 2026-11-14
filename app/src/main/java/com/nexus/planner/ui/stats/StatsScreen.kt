package com.nexus.planner.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.planner.data.local.entity.HabitEntity
import com.nexus.planner.ui.theme.*

@Composable
fun StatsScreen(
    viewModel: StatsViewModel,
    modifier: Modifier = Modifier
) {
    val habits by viewModel.habits.collectAsState()
    val frictions by viewModel.frictions.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Black)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp)
    ) {
        item {
            Column {
                Text(
                    text = "HABIT TELEMETRY",
                    style = MaterialTheme.typography.labelSmall,
                    color = Zinc400
                )
                Text(
                    text = "66-Day Automaticity",
                    style = MaterialTheme.typography.headlineMedium
                )
            }
            Divider(color = Zinc900, thickness = 1.dp, modifier = Modifier.padding(top = 8.dp))
        }

        // Habit Cards
        items(habits, key = { it.id }) { habit ->
            HabitAutomaticityCard(habit = habit)
        }

        // Friction Summary Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Zinc800, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Zinc950),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("FRICTION AUDIT SUMMARY", style = MaterialTheme.typography.labelSmall, color = Zinc400)
                    Text("Top friction logged: Post-Work Phone Scrolling (54% of delays)", style = MaterialTheme.typography.bodyMedium, color = Zinc200)
                    Text("AI Adaptation: 15:30 Transition Interceptor reduces subsequent inertia.", style = MaterialTheme.typography.bodyMedium, color = EmeraldAccent)
                }
            }
        }
    }
}

@Composable
fun HabitAutomaticityCard(habit: HabitEntity) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Zinc800, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Zinc950),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(habit.name, style = MaterialTheme.typography.titleMedium, color = White)
                Text("Day ${habit.currentDayIndex} / 66", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Zinc400)
            }

            LinearProgressIndicator(
                progress = habit.automaticityScore.coerceIn(0f, 1f),
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = White,
                trackColor = Zinc900
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (habit.currentDayIndex < 23) "Phase 1: Resistance" else if (habit.currentDayIndex < 45) "Phase 2: Installation" else "Phase 3: Automaticity",
                    style = MaterialTheme.typography.labelSmall,
                    color = Zinc500
                )
                Text(
                    text = "${(habit.automaticityScore * 100).toInt()}% automatic",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = Zinc400
                )
            }
        }
    }
}
