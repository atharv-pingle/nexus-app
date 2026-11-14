package com.nexus.planner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.planner.data.local.entity.ScheduleBlockEntity
import com.nexus.planner.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkipFixModalSheet(
    block: ScheduleBlockEntity?,
    onGentleSwap: (ScheduleBlockEntity, String) -> Unit,
    onShiftLater: (ScheduleBlockEntity) -> Unit,
    onDismiss: () -> Unit
) {
    if (block == null) return

    val gentleAlternative = when {
        block.category.contains("FITNESS", ignoreCase = true) || block.category.contains("WORKOUT", ignoreCase = true) ->
            "5-minute mobility stretch & 10 pushups"
        block.category.contains("DEEP_WORK", ignoreCase = true) || block.category.contains("FOCUS", ignoreCase = true) || block.category.contains("SAAS", ignoreCase = true) ->
            "5-minute task breakdown: write 1 next actionable step"
        block.category.contains("SLEEP", ignoreCase = true) || block.category.contains("TRANSITION", ignoreCase = true) ->
            "3-minute box breathing: 4s in, 4s hold, 4s out"
        else -> "5-minute non-zero micro-action"
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Zinc950,
        contentColor = White,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 12.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Zinc700)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
        ) {
            // Tactical Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⚡ SKIP-FIX INTERCEPTOR",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = AmberAccent
                )
                ChamferTag(text = "STREAK SHIELD", colorAccent = AmberAccent)
            }

            Text(
                text = "Low energy or pressed for time? Don't break the 66-day habit chain. Select an intervention below:",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = Zinc400,
                modifier = Modifier.padding(top = 6.dp, bottom = 20.dp)
            )

            // Current Task Overview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(Zinc900)
                    .border(1.dp, Zinc800, RoundedCornerShape(6.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Text(
                        text = "CURRENT TASK // ${block.durationMinutes} MINS",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        color = Zinc500
                    )
                    Text(
                        text = block.title,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Zinc200,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Option 1: Gentle Swap
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CutCornerShape(6.dp))
                    .background(Zinc900)
                    .border(1.dp, EmeraldAccent.copy(alpha = 0.5f), CutCornerShape(6.dp))
                    .clickable { onGentleSwap(block, gentleAlternative) }
                    .padding(14.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "1. GENTLE SWAP (5-MIN MICRO-HABIT)",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldAccent
                        )
                        ChamferTag(text = "RECOMMENDED", colorAccent = EmeraldAccent)
                    }

                    Text(
                        text = gentleAlternative,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = White,
                        modifier = Modifier.padding(top = 6.dp)
                    )

                    Text(
                        text = "Reduces effort to 5 mins. Marks block complete & keeps your streak intact.",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = Zinc400,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Option 2: Shift Schedule Later
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CutCornerShape(6.dp))
                    .background(Zinc900)
                    .border(1.dp, Zinc800, CutCornerShape(6.dp))
                    .clickable { onShiftLater(block) }
                    .padding(14.dp)
            ) {
                Column {
                    Text(
                        text = "2. SHIFT +30M TO LATER",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Zinc200
                    )
                    Text(
                        text = "Push this block and remaining afternoon schedule forward by 30 minutes.",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = Zinc500,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Dismiss Button
            Text(
                text = "CANCEL",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = Zinc500,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clickable(onClick = onDismiss)
                    .padding(8.dp)
            )
        }
    }
}
