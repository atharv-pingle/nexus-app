package com.nexus.planner.ui.coach

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.planner.data.remote.ServerChatMessageDto
import com.nexus.planner.data.remote.ServerScheduleBlockDto
import com.nexus.planner.ui.theme.*
import kotlinx.coroutines.delay
import org.json.JSONArray

@Composable
fun ChatAssistantScreen(
    viewModel: ChatAssistantViewModel,
    onStartVoiceRecognition: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Auto-scroll when new messages arrive or when agent is processing
    LaunchedEffect(state.messages.size, state.isLoading) {
        val count = state.messages.size + (if (state.isLoading) 1 else 0)
        if (count > 0) {
            listState.animateScrollToItem(count)
        }
    }

    // Auto-clear toast
    LaunchedEffect(state.toastNotice) {
        if (state.toastNotice != null) {
            delay(2500)
            viewModel.clearToast()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Black)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Message Stream
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                // Date Header
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "TODAY • ACTIVE CONVERSATION",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Zinc500
                        )
                    }
                }

                // Render Messages
                items(state.messages) { msg ->
                    if (msg.sender == "user") {
                        UserMessageBubble(msg.content)
                    } else {
                        AgentMessageBubble(
                            content = msg.content,
                            actionsJson = msg.actions_json,
                            onActionClick = { action, blockId, mins ->
                                if (action == "INJECT_PUSHUPS") {
                                    viewModel.recordPushups(10)
                                } else {
                                    viewModel.executeQuickAction(action, blockId, mins)
                                }
                            }
                        )
                    }
                }

                // In-line typing / processing indicator
                if (state.isLoading) {
                    item {
                        AgentTypingBubble()
                    }
                }

                // Embedded Task List Section (Only shown on-demand when user asks)
                if (state.showScheduleInChat && state.todaySchedule.isNotEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "SCHEDULE PREVIEW",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = Emerald400
                                )
                                Text(
                                    text = "DISMISS",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = Zinc500,
                                    modifier = Modifier.clickable { viewModel.dismissScheduleFromChat() }
                                )
                            }
                            TaskListSection(
                                tasks = state.todaySchedule,
                                onToggle = { blockId -> viewModel.toggleTask(blockId) },
                                onShift = { mins -> viewModel.shiftSchedule(mins) }
                            )
                        }
                    }
                }
            }

            // Bottom ChatGPT-Style Input Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Black,
                border = BorderStroke(1.dp, Zinc900)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Message Nexus...", fontSize = 14.sp, color = Zinc500) },
                        modifier = Modifier.weight(1f),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White, fontSize = 14.5.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Zinc600,
                            unfocusedBorderColor = Zinc800,
                            focusedContainerColor = Zinc950,
                            unfocusedContainerColor = Zinc950
                        ),
                        shape = RoundedCornerShape(20.dp),
                        singleLine = true
                    )

                    // Circular White Mic Button (ChatGPT Style)
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .clickable {
                                viewModel.openVoiceModal()
                                onStartVoiceRecognition()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice",
                            tint = Color.Black,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Send Button
                    if (inputText.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Zinc800)
                                .clickable {
                                    val text = inputText
                                    inputText = ""
                                    viewModel.sendMessage(text)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "↑",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Floating Agent Processing Pop-up Pill
        AnimatedVisibility(
            visible = state.isLoading,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 74.dp)
        ) {
            Surface(
                color = Color(0xFF18181B),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Emerald400.copy(alpha = 0.6f)),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "⚡", fontSize = 12.sp)
                    Text(
                        text = "Nexus is processing...",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Emerald400
                    )
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        strokeWidth = 1.5.dp,
                        color = Emerald400
                    )
                }
            }
        }

        // Quick Toast Notice
        state.toastNotice?.let { notice ->
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 60.dp, start = 20.dp, end = 20.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Zinc900)
                    .border(1.dp, Zinc700, RoundedCornerShape(8.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = notice,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = Color.White
                )
            }
        }

        // Voice Orb Modal
        if (state.isVoiceModalOpen) {
            VoiceOrbScreen(
                liveText = state.liveSpeechText,
                onClose = { viewModel.closeVoiceModal() },
                onSelectExample = { text -> viewModel.updateLiveSpeech(text) },
                onConfirm = { viewModel.confirmVoice() }
            )
        }
    }
}

@Composable
fun TaskListSection(
    tasks: List<ServerScheduleBlockDto>,
    onToggle: (Long) -> Unit,
    onShift: (Int) -> Unit
) {
    val completedCount = tasks.count { it.is_completed }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Zinc950,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Zinc800)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TODAY'S TASKS",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = Zinc400
                )
                Text(
                    text = "$completedCount of ${tasks.size} completed",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = Zinc500
                )
            }

            // Task list items
            tasks.forEach { task ->
                val isCompleted = task.is_completed
                val blockId = task.id ?: 0L

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isCompleted) Zinc900.copy(alpha = 0.4f) else Zinc900)
                        .border(1.dp, if (isCompleted) Zinc900 else Zinc800, RoundedCornerShape(8.dp))
                        .clickable { onToggle(blockId) }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Checkbox Circle
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(if (isCompleted) Emerald400.copy(alpha = 0.2f) else Color.Transparent)
                            .border(
                                1.5.dp,
                                if (isCompleted) Emerald400 else Zinc600,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCompleted) {
                            Text(
                                text = "✓",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Emerald400
                            )
                        }
                    }

                    // Title & Time
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = task.title,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isCompleted) Zinc500 else Color.White,
                            textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None
                        )
                        Text(
                            text = "${task.start_time} – ${task.end_time}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = if (isCompleted) Zinc600 else Zinc400
                        )
                    }

                    // Tag
                    if (task.block_type == "FITNESS_NUDGE") {
                        Text(
                            text = "RESET",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Emerald400
                        )
                    }
                }
            }

            // Safe Divider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Zinc900)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Need more time?",
                    fontSize = 11.sp,
                    color = Zinc500
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        color = Zinc900,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, Zinc800),
                        modifier = Modifier.clickable { onShift(15) }
                    ) {
                        Text(
                            text = "+15 min",
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
                        modifier = Modifier.clickable { onShift(30) }
                    ) {
                        Text(
                            text = "+30 min",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Zinc300,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UserMessageBubble(content: String) {
    val cleanContent = remember(content) { content.replace("**", "") }
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterEnd
    ) {
        Surface(
            color = Zinc800,
            shape = RoundedCornerShape(topStart = 18.dp, topEnd = 4.dp, bottomStart = 18.dp, bottomEnd = 18.dp),
            border = BorderStroke(1.dp, Zinc700),
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Text(
                text = cleanContent,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Normal,
                color = Color.White,
                lineHeight = 21.sp,
                modifier = Modifier.padding(14.dp)
            )
        }
    }
}

data class ActionChipData(
    val label: String,
    val action: String,
    val blockId: Long? = null,
    val mins: Int? = null
)

@Composable
fun AgentMessageBubble(
    content: String,
    actionsJson: String?,
    onActionClick: (String, Long?, Int?) -> Unit
) {
    val cleanContent = remember(content) { content.replace("**", "") }
    val actionChips = remember(actionsJson) {
        val list = mutableListOf<ActionChipData>()
        if (!actionsJson.isNullOrBlank()) {
            try {
                val arr = JSONArray(actionsJson)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val label = obj.optString("label", "Action")
                    val action = obj.optString("action", "")
                    val blockId = if (obj.has("block_id")) obj.getLong("block_id") else null
                    val mins = if (obj.has("mins")) obj.getInt("mins") else null
                    list.add(ActionChipData(label, action, blockId, mins))
                }
            } catch (e: Exception) {
                // Ignore parse errors safely
            }
        }
        list
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(Emerald400),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "N",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = Color.Black
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                color = Zinc900,
                shape = RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp),
                border = BorderStroke(1.dp, Zinc800)
            ) {
                Text(
                    text = cleanContent,
                    fontSize = 14.5.sp,
                    color = Zinc100,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(14.dp)
                )
            }

            // Quick Action Chips
            if (actionChips.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    actionChips.forEach { chip ->
                        Surface(
                            color = Zinc900,
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, Zinc700),
                            modifier = Modifier.clickable { onActionClick(chip.action, chip.blockId, chip.mins) }
                        ) {
                            Text(
                                text = chip.label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AgentTypingBubble() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing_dots")

    val dot1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1Alpha"
    )
    val dot2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, delayMillis = 160, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2Alpha"
    )
    val dot3Alpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, delayMillis = 320, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3Alpha"
    )

    val dot1Scale by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1Scale"
    )
    val dot2Scale by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, delayMillis = 160, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2Scale"
    )
    val dot3Scale by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, delayMillis = 320, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3Scale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(Emerald400),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "N",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = Color.Black
            )
        }

        Surface(
            color = Zinc900,
            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp),
            border = BorderStroke(1.dp, Zinc800)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .graphicsLayer {
                                scaleX = dot1Scale
                                scaleY = dot1Scale
                                alpha = dot1Alpha
                            }
                            .clip(CircleShape)
                            .background(Emerald400)
                    )
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .graphicsLayer {
                                scaleX = dot2Scale
                                scaleY = dot2Scale
                                alpha = dot2Alpha
                            }
                            .clip(CircleShape)
                            .background(Emerald400)
                    )
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .graphicsLayer {
                                scaleX = dot3Scale
                                scaleY = dot3Scale
                                alpha = dot3Alpha
                            }
                            .clip(CircleShape)
                            .background(Emerald400)
                    )
                }

                Text(
                    text = "Nexus is thinking...",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = Zinc400
                )
            }
        }
    }
}

@Composable
fun VoiceOrbScreen(
    liveText: String,
    onClose: () -> Unit,
    onSelectExample: (String) -> Unit,
    onConfirm: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.96f))
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Emerald400))
                    Text("NEXUS VOICE", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Zinc400)
                }
                Text(
                    text = "Close",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = Zinc500,
                    modifier = Modifier.clickable { onClose() }
                )
            }

            // Central Orb
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Mic",
                        tint = Color.Black,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Text(
                    text = if (liveText.isNotBlank()) "\"$liveText\"" else "Listening to your voice...",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )

                // Quick Tap Examples
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Or tap a quick command:",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Zinc500
                    )
                    Surface(
                        color = Zinc950,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Zinc800),
                        modifier = Modifier.fillMaxWidth().clickable { onSelectExample("Finished 10 pushups") }
                    ) {
                        Text(
                            text = "\"Finished 10 pushups\"",
                            fontSize = 11.sp,
                            color = Zinc300,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    Surface(
                        color = Zinc950,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Zinc800),
                        modifier = Modifier.fillMaxWidth().clickable { onSelectExample("Meeting ran 20m over, shift afternoon") }
                    ) {
                        Text(
                            text = "\"Meeting ran 20m over, shift afternoon\"",
                            fontSize = 11.sp,
                            color = Zinc300,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }

            // Confirm Button
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(46.dp)
            ) {
                Text("Confirm & Execute", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}
