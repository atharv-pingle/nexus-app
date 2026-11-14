package com.nexus.planner.ui.timeline

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.planner.data.local.entity.ScheduleBlockEntity
import com.nexus.planner.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun TimelineScreen(
    viewModel: TimelineViewModel,
    modifier: Modifier = Modifier
) {
    val schedule by viewModel.schedule.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    val activeBlock = schedule.firstOrNull { it.status == "ACTIVE" } ?: schedule.firstOrNull()
    val remainingBlocks = schedule.filter { it.id != activeBlock?.id && it.status != "COMPLETED" }

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
                            text = LocalDate.now().format(DateTimeFormatter.ofPattern("EEE dd MMM")).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = Zinc400
                        )
                        Text(
                            text = "Daily Orbit",
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                    Text(
                        text = "SYNCED",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = EmeraldAccent
                    )
                }
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Zinc900))
            }

            // Active Focus Block
            if (activeBlock != null) {
                item {
                    ActiveBlockCard(
                        block = activeBlock,
                        onAddDelay = { viewModel.addDelay(it) },
                        onCompleteStep = { viewModel.completeMicroAction(activeBlock.id) },
                        onSkip = { viewModel.skipBlock(activeBlock.id) }
                    )
                }
            }

            // 15:30 Transition Interceptor Card
            item {
                TransitionInterceptorCard(
                    onStartNap = { viewModel.startNapTimer() }
                )
            }

            // Upcoming Schedule Stream
            item {
                Text(
                    text = "UPCOMING SESSIONS",
                    style = MaterialTheme.typography.labelSmall,
                    color = Zinc500,
                    modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                )
            }

            items(remainingBlocks, key = { it.id }) { block ->
                ScheduleRowItem(block = block)
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
                        Text("OK", color = Zinc300)
                    }
                }
            ) {
                Text(statusMessage!!, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun ActiveBlockCard(
    block: ScheduleBlockEntity,
    onAddDelay: (Int) -> Unit,
    onCompleteStep: () -> Unit,
    onSkip: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Zinc800, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Zinc950),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(EmeraldAccent))
                    Text(
                        text = "ACTIVE",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldAccent
                    )
                }
                Text(
                    text = "${block.startTime} – ${block.endTime}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Zinc400
                )
            }

            Column {
                Text(
                    text = block.title,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = block.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Zinc400
                )
            }

            // 2-Minute Micro Action Launcher
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Zinc900, RoundedCornerShape(8.dp))
                    .border(1.dp, Zinc800, RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = "NEXT 2-MIN ACTION:",
                            style = MaterialTheme.typography.labelSmall,
                            color = Zinc400
                        )
                        Text(
                            text = block.microAction,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = Zinc100
                        )
                    }
                    Button(
                        onClick = onCompleteStep,
                        colors = ButtonDefaults.buttonColors(containerColor = White, contentColor = Black),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text("Done", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            // Tactile Delay & Skip Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = { onAddDelay(15) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Zinc300),
                    border = BorderStroke(1.dp, Zinc800)
                ) {
                    Text("+15m", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = { onAddDelay(30) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Zinc300),
                    border = BorderStroke(1.dp, Zinc800)
                ) {
                    Text("+30m", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = onSkip,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Zinc400),
                    border = BorderStroke(1.dp, Zinc800)
                ) {
                    Text("Skip", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun TransitionInterceptorCard(onStartNap: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Zinc800, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Zinc950),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("15:30", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = AmberAccent)
                    Text("Work-Life Reset", style = MaterialTheme.typography.titleMedium)
                }
                Text("20-min power nap / NSDR timer", style = MaterialTheme.typography.bodyMedium, color = Zinc400)
            }
            OutlinedButton(
                onClick = onStartNap,
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberAccent),
                border = BorderStroke(1.dp, AmberAccent.copy(alpha = 0.4f))
            ) {
                Text("Start 20m", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun ScheduleRowItem(block: ScheduleBlockEntity) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Zinc900, RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = Zinc950),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(block.startTime, style = MaterialTheme.typography.labelSmall, color = Zinc400)
                    Text(block.title, style = MaterialTheme.typography.titleMedium, color = Zinc100)
                }
                Text(
                    text = "${block.durationMinutes}m • ${block.description}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Zinc500
                )
            }
            Text(
                text = "${block.durationMinutes}m",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = Zinc400
            )
        }
    }
}
