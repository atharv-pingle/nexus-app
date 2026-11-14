package com.nexus.planner.ui.coach

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
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
import com.nexus.planner.data.remote.ServerChatMessageDto
import com.nexus.planner.data.remote.ServerScheduleBlockDto
import com.nexus.planner.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    val coroutineScope = rememberCoroutineScope()

    // Auto-scroll when new messages arrive
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size)
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

            // Top App Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                color = Black,
                border = BorderStroke(1.dp, Zinc900)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Emerald400)
                        )
                        Text(
                            text = "NEXUS // LIVE ASSISTANT",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White
                        )
                    }

                    IconButton(onClick = { viewModel.loadData() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = Zinc400,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

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
                            text = "TODAY • ACTIVE TIMELINE",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Zinc500
                        )
                    }
                }

                // Render First Agent Message + Embedded Daily Task List
                items(state.messages) { msg ->
                    if (msg.sender == "user") {
                        UserMessageBubble(msg.content)
                    } else {
                        AgentMessageBubble(
                            content = msg.content,
                            actionsJson = msg.actions_json,
                            onActionClick = { action, blockId, mins ->
                                viewModel.executeQuickAction(action, blockId, mins)
                            }
                        )
                    }
                }

                // Embedded Task List Section (Pinned under initial context)
                if (state.schedule.isNotEmpty()) {
                    item {
                        TaskListSection(
                            tasks = state.schedule,
                            onToggle = { blockId -> viewModel.toggleTask(blockId) },
                            onShift = { mins -> viewModel.shiftSchedule(mins) }
                        )
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
                    // Pill text field
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(Zinc900)
                            .border(1.dp, Zinc800, RoundedCornerShape(22.dp))
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        TextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = {
                                Text(
                                    "Message Nexus...",
                                    fontSize = 13.sp,
                                    color = Zinc500
                                )
                            },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

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
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Zinc800)
                                .clickable {
                                    val text = inputText
                                    inputText = ""
                                    viewModel.sendMessage(text)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Send",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
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

            // Quick Delay Controls
            Divider(color = Zinc900, thickness = 1.dp)

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
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterEnd
    ) {
        Surface(
            color = Zinc900,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
            border = BorderStroke(1.dp, Zinc800),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = content,
                fontSize = 12.sp,
                color = Color.White,
                lineHeight = 18.sp,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

@Composable
fun AgentMessageBubble(
    content: String,
    actionsJson: String?,
    onActionClick: (String, Long?, Int?) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(Zinc800)
                .border(1.dp, Zinc700, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "N",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = Color.White
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                color = Zinc950,
                shape = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
                border = BorderStroke(1.dp, Zinc800)
            ) {
                Text(
                    text = content,
                    fontSize = 12.sp,
                    color = Zinc300,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }

            // Quick Action Chips
            if (!actionsJson.isNullOrBlank()) {
                try {
                    val arr = JSONArray(actionsJson)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            val label = obj.optString("label", "Action")
                            val action = obj.optString("action", "")
                            val blockId = if (obj.has("block_id")) obj.getLong("block_id") else null
                            val mins = if (obj.has("mins")) obj.getInt("mins") else null

                            Surface(
                                color = Zinc900,
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, Zinc700),
                                modifier = Modifier.clickable { onActionClick(action, blockId, mins) }
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Ignore JSON parse errors safely
                }
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
