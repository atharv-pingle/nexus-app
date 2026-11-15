package com.nexus.planner.ui.stats

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.planner.ui.theme.*

@Composable
fun StatsScreen(
    pushupsCount: Int = 20,
    workoutStreak: Int = 19,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Black)
            .padding(horizontal = 16.dp)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 14.dp, bottom = 40.dp)
        ) {
            // Header Bar
            item {
                Column(modifier = Modifier.padding(bottom = 4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("📈", fontSize = 16.sp)
                        Text(
                            text = "AI AGENT TELEMETRY",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = Color.White
                        )
                    }
                    Text(
                        text = "Focus planning velocity & execution status",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Zinc500,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Zinc900)
                )
            }

            // Daily Executive Brief & Motivational Momentum Card
            item {
                Surface(
                    color = Zinc950,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Zinc800)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("🎯", fontSize = 14.sp)
                                Text(
                                    text = "DAILY EXECUTIVE BRIEF",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    letterSpacing = 0.5.sp
                                )
                            }
                            Text(
                                text = "NEXUS AI",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Emerald400
                            )
                        }

                        Text(
                            text = "You're operating at high momentum. Your deep work blocks and physical resets are aligned. Protect your evening wind-down and maintain consistency.",
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            color = Zinc300
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⚡ Momentum High",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = Emerald400
                            )
                            Text(
                                text = "💪 $pushupsCount Pushups Logged",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = Zinc400
                            )
                        }
                    }
                }
            }

            // Focus Planning Velocity Card (Replaced 66-day habit challenge)
            item {
                Surface(
                    color = Zinc950,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Zinc800)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "FOCUS PLANNING VELOCITY",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Zinc400,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "85% Optimized",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Emerald400
                            )
                        }

                        // Progress bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .background(Zinc900, RoundedCornerShape(4.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .fillMaxHeight()
                                    .background(Emerald400, RoundedCornerShape(4.dp))
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Today: 3h 45m Focused",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = Zinc300
                            )
                            Text(
                                text = "Tomorrow: 4h 15m Planned",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Text(
                            text = "When you snooze or postpone tasks, Nexus shifts subsequent blocks dynamically without cognitive friction.",
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            color = Zinc400
                        )
                    }
                }
            }

            // Nightly Architect & Core Brain Rollover
            item {
                Surface(
                    color = Zinc950,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Zinc800)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "CORE BRAIN ROLLOVER",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Zinc400,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "🔄 Rollover Active",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFBBF24)
                            )
                        }
                        Text(
                            text = "Any unfinished tasks from today automatically carry over into tomorrow's priority morning queue so nothing falls through the cracks.",
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            color = Zinc300
                        )
                    }
                }
            }

            // Metrics Row: Pushups & Bedtime
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Pushups Today Card
                    Surface(
                        color = Zinc950,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Zinc800),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "PUSHUPS TODAY",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Zinc500,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "$pushupsCount reps",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFBBF24)
                            )
                            Text(
                                text = "${pushupsCount / 10} physical resets",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = Zinc400
                            )
                        }
                    }

                    // Bedtime Lock Card
                    Surface(
                        color = Zinc950,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Zinc800),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "BEDTIME LOCK",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Zinc500,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "23:30",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF60A5FA)
                            )
                            Text(
                                text = "Wind-down: 22:30",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF93C5FD)
                            )
                        }
                    }
                }
            }
        }
    }
}
