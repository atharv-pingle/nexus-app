package com.nexus.planner.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nexus.planner.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun WorkoutRestModal(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var restSeconds by remember { mutableIntStateOf(90) }
    var remainingSeconds by remember { mutableIntStateOf(90) }
    var isRunning by remember { mutableStateOf(false) }

    LaunchedEffect(isRunning, remainingSeconds) {
        if (isRunning && remainingSeconds > 0) {
            delay(1000L)
            remainingSeconds -= 1
        } else if (isRunning && remainingSeconds == 0) {
            isRunning = false
            triggerRestFinishedVibration(context)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Black)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                ChamferTag(text = "DUMBBELL & BENCH TRACKER", colorAccent = AmberAccent)

                Text(
                    text = "REST TIMER",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = White,
                    modifier = Modifier.padding(top = 10.dp)
                )

                Text(
                    text = "Autobeeps and vibrates when rest interval is finished.",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = Zinc400,
                    modifier = Modifier.padding(top = 4.dp, bottom = 32.dp)
                )

                // Timer Display Circle
                Box(
                    modifier = Modifier
                        .size(190.dp)
                        .clip(CircleShape)
                        .background(Zinc950)
                        .border(2.dp, if (isRunning) AmberAccent else Zinc700, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${remainingSeconds}s",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (remainingSeconds <= 5 && isRunning) RedAccent else White
                        )
                        Text(
                            text = if (isRunning) "RESTING..." else "READY",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            letterSpacing = 1.sp,
                            color = if (isRunning) AmberAccent else Zinc500,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Presets Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    listOf(45, 60, 90, 120).forEach { s ->
                        val selected = restSeconds == s
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (selected) Zinc800 else Zinc950)
                                .border(1.dp, if (selected) White else Zinc800, RoundedCornerShape(4.dp))
                                .clickable {
                                    restSeconds = s
                                    remainingSeconds = s
                                    isRunning = false
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${s}s",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) White else Zinc400
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Control Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    ChamferButton(
                        text = if (isRunning) "PAUSE" else "START REST",
                        onClick = { isRunning = !isRunning },
                        isPrimary = true,
                        colorAccent = AmberAccent,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )
                    ChamferButton(
                        text = "RESET",
                        onClick = { remainingSeconds = restSeconds; isRunning = false },
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "CLOSE WORKOUT TRACKER",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = Zinc500,
                    modifier = Modifier
                        .clickable(onClick = onDismiss)
                        .padding(8.dp)
                )
            }
        }
    }
}

private fun triggerRestFinishedVibration(context: Context) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator?.vibrate(
                VibrationEffect.createWaveform(longArrayOf(0, 150, 100, 150, 100, 300), -1)
            )
        } else {
            @Suppress("DEPRECATION")
            val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            @Suppress("DEPRECATION")
            v?.vibrate(longArrayOf(0, 150, 100, 150, 100, 300), -1)
        }
    } catch (_: Exception) {}
}
