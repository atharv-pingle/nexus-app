package com.nexus.planner.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.planner.service.VoiceInteractionManager
import com.nexus.planner.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceModalSheet(
    voiceManager: VoiceInteractionManager,
    onIntentConfirmed: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val isListening by voiceManager.isListening.collectAsState()
    val transcription by voiceManager.transcription.collectAsState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Black,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(EmeraldAccent))
                    Text(
                        text = if (isListening) "LISTENING..." else "READY",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = EmeraldAccent
                    )
                }
                TextButton(onClick = onDismiss) {
                    Text("Close", color = Zinc400, fontFamily = FontFamily.Monospace)
                }
            }

            // Pulsing Mic Icon
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Zinc900)
                    .border(2.dp, if (isListening) EmeraldAccent else Zinc800, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                IconButton(onClick = {
                    if (isListening) {
                        voiceManager.stopListening()
                    } else {
                        voiceManager.startListening { text ->
                            onIntentConfirmed(text)
                        }
                    }
                }) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Microphone",
                        tint = if (isListening) EmeraldAccent else White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Text(
                text = if (transcription.isBlank()) "Tap mic and say: 'Shift evening by 30 mins' or 'Start power nap'" else "\"$transcription\"",
                style = MaterialTheme.typography.bodyLarge,
                color = if (transcription.isBlank()) Zinc500 else White,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            // Quick Voice Action Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("QUICK PROMPTS", style = MaterialTheme.typography.labelSmall, color = Zinc500)
                listOf(
                    "Shift evening blocks by 30 minutes",
                    "Finished work, start 20m power nap",
                    "Completed push workout with 20kg dumbbells"
                ).forEach { prompt ->
                    OutlinedButton(
                        onClick = { onIntentConfirmed(prompt) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Zinc300),
                        border = BorderStroke(1.dp, Zinc900)
                    ) {
                        Text(prompt, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
