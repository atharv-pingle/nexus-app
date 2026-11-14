package com.nexus.planner.ui.coach

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.planner.ui.theme.*

@Composable
fun CoachScreen(
    viewModel: CoachViewModel,
    modifier: Modifier = Modifier
) {
    val messages by viewModel.messages.collectAsState()
    val isSending by viewModel.isSending.collectAsState()
    var inputText by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Black)
            .padding(horizontal = 16.dp)
    ) {
        // Top Header
        Column(modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)) {
            Text(
                text = "DIRECT AGENT DIALOGUE",
                style = MaterialTheme.typography.labelSmall,
                color = Zinc400
            )
            Text(
                text = "Autonomous Coach",
                style = MaterialTheme.typography.headlineMedium
            )
            Divider(color = Zinc900, thickness = 1.dp, modifier = Modifier.padding(top = 8.dp))
        }

        // Message Feed
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            items(messages) { msg ->
                val isAgent = msg.sender == "AGENT"
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = if (isAgent) Alignment.CenterStart else Alignment.CenterEnd
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .border(1.dp, if (isAgent) Zinc800 else Zinc700, RoundedCornerShape(8.dp)),
                        colors = CardDefaults.cardColors(containerColor = if (isAgent) Zinc950 else Zinc900),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = msg.sender,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = if (isAgent) Zinc500 else Zinc400
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = msg.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isAgent) Zinc200 else White
                            )
                        }
                    }
                }
            }

            if (isSending) {
                item {
                    Text(
                        text = "AGENT IS REASONING...",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Zinc500,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }

        // Input Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 80.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("Ask coach or modify plan...", fontSize = 12.sp, color = Zinc500) },
                modifier = Modifier.weight(1f),
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = White),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Zinc500,
                    unfocusedBorderColor = Zinc800,
                    focusedContainerColor = Zinc950,
                    unfocusedContainerColor = Zinc950
                ),
                shape = RoundedCornerShape(8.dp)
            )

            Button(
                onClick = {
                    viewModel.sendMessage(inputText)
                    inputText = ""
                },
                enabled = inputText.isNotBlank() && !isSending,
                colors = ButtonDefaults.buttonColors(containerColor = White, contentColor = Black),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Send", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            }
        }
    }
}
