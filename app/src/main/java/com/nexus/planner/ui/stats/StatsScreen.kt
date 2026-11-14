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
    uclDay: Int = 20,
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
                        Text("📈", fontSize = 14.sp)
                        Text(
                            text = "NEXUS TELEMETRY",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = Color.White
                        )
                    }
                    Text(
                        text = "UCL automaticity and habit telemetry",
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

            // UCL Automaticity Card (Zero Text Break, Solid Padding)
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
                                text = "AUTOMATICITY SCORE",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Zinc400,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "Day $uclDay of 66",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Emerald400
                            )
                        }

                        // Progress bar (UCL 66-day protocol: 20/66 ~ 30%)
                        val progressFraction = (uclDay.toFloat() / 66f).coerceIn(0.05f, 1f)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .background(Zinc900, RoundedCornerShape(4.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progressFraction)
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
                                text = "Phase 1: Friction Reduction",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = Zinc300
                            )
                            Text(
                                text = "${(progressFraction * 100).toInt()}% Locked",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Emerald400
                            )
                        }

                        Text(
                            text = "Habits are transitioning into automatic reflexes with 42% lower daily resistance.",
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            color = Zinc400
                        )
                    }
                }
            }

            // Metrics Row: Pushups & Workout Streak
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
                                color = Color(0xFFFBBF24) // Amber 400
                            )
                            Text(
                                text = "${pushupsCount / 10} challenges met",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = Zinc400
                            )
                        }
                    }

                    // Workout Streak Card
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
                                text = "WORKOUT STREAK",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Zinc500,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "$workoutStreak Days",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Shield active",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Emerald400
                            )
                        }
                    }
                }
            }

            // Bedtime Recovery Lock Card
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
                                text = "BEDTIME LOCK",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Zinc400,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "23:30 (Wind-down: 22:30)",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF60A5FA)
                            )
                        }
                        Text(
                            text = "Nexus initiates screen wind-down 60 minutes prior to bedtime to ensure uninterrupted delta-wave sleep recovery.",
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            color = Zinc400
                        )
                    }
                }
            }

            // Friction Adaptation Summary
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
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "AI BEHAVIORAL INTERVENTION",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Zinc500
                        )
                        Text(
                            text = "Transition Nap Interceptor (15:30) successfully eliminated afternoon procrastination blocks.",
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            color = Emerald400
                        )
                    }
                }
            }
        }
    }
}
