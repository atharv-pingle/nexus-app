package com.nexus.planner.ui.architect

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.planner.ui.theme.*

@Composable
fun ArchitectScreen(
    viewModel: ArchitectViewModel,
    modifier: Modifier = Modifier
) {
    val selectedEnergy by viewModel.selectedEnergy.collectAsState()
    val selectedFrictions by viewModel.selectedFrictions.collectAsState()
    val priorityGoal by viewModel.priorityGoal.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val resultReasoning by viewModel.generationResult.collectAsState()

    val availableFrictions = listOf(
        "Phone Scroll",
        "Post-Work Slump",
        "Work Overran",
        "Vague Task"
    )

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
                    text = "REFLECT & BUILD",
                    style = MaterialTheme.typography.labelSmall,
                    color = Zinc400
                )
                Text(
                    text = "Tomorrow's Architecture",
                    style = MaterialTheme.typography.headlineMedium
                )
            }
            Divider(color = Zinc900, thickness = 1.dp, modifier = Modifier.padding(top = 8.dp))
        }

        // Section: Energy Level
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Zinc800, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Zinc950),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("ENERGY LEVEL TODAY", style = MaterialTheme.typography.labelSmall, color = Zinc400)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("HIGH", "MEDIUM", "LOW").forEach { level ->
                            val isSelected = selectedEnergy == level
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (isSelected) White else Zinc900,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) White else Zinc800,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable { viewModel.selectEnergy(level) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = androidx.compose.ui.Alignment.Center
                            ) {
                                Text(
                                    text = level,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Black else Zinc300
                                )
                            }
                        }
                    }

                    // Section: Friction Points
                    Text("FRICTION POINTS (TAP TO LOG)", style = MaterialTheme.typography.labelSmall, color = Zinc400, modifier = Modifier.padding(top = 6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        availableFrictions.take(2).forEach { tag ->
                            val isSelected = selectedFrictions.contains(tag)
                            FrictionChip(tag = tag, isSelected = isSelected, onClick = { viewModel.toggleFriction(tag) }, modifier = Modifier.weight(1f))
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        availableFrictions.drop(2).forEach { tag ->
                            val isSelected = selectedFrictions.contains(tag)
                            FrictionChip(tag = tag, isSelected = isSelected, onClick = { viewModel.toggleFriction(tag) }, modifier = Modifier.weight(1f))
                        }
                    }

                    // Section: Goal Input
                    Text("TOMORROW'S #1 PRIORITY GOAL", style = MaterialTheme.typography.labelSmall, color = Zinc400, modifier = Modifier.padding(top = 6.dp))
                    OutlinedTextField(
                        value = priorityGoal,
                        onValueChange = { viewModel.setPriorityGoal(it) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = White, fontFamily = FontFamily.Monospace),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Zinc500,
                            unfocusedBorderColor = Zinc800,
                            focusedContainerColor = Zinc900,
                            unfocusedContainerColor = Zinc900
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
        }

        // Generate Button
        item {
            Button(
                onClick = { viewModel.generateTomorrowPlan() },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                enabled = !isGenerating,
                colors = ButtonDefaults.buttonColors(containerColor = White, contentColor = Black),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Black, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("SYNTHESIZING WITH AGENT...", fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                } else {
                    Text("GENERATE NEXT DAY SCHEDULE", fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Result Card
        if (resultReasoning != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, EmeraldAccent.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = Zinc950),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("AI SYNTHESIS COMPLETE", style = MaterialTheme.typography.labelSmall, color = EmeraldAccent)
                            Text("TOMORROW READY", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Zinc400)
                        }
                        Text(
                            text = resultReasoning!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Zinc200
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FrictionChip(
    tag: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(if (isSelected) Zinc800 else Zinc900, RoundedCornerShape(6.dp))
            .border(1.dp, if (isSelected) RedAccent.copy(alpha = 0.6f) else Zinc800, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 6.dp),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Text(
            text = tag,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = if (isSelected) White else Zinc400
        )
    }
}
