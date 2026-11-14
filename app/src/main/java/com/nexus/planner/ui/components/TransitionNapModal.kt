package com.nexus.planner.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.scale
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
fun TransitionNapModal(
    onDismiss: () -> Unit,
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    var totalSeconds by remember { mutableIntStateOf(20 * 60) } // 20 minutes
    var isRunning by remember { mutableStateOf(true) }

    // Breathing phase animation: Inhale (4s), Hold (4s), Exhale (6s)
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Countdown effect
    LaunchedEffect(isRunning, totalSeconds) {
        if (isRunning && totalSeconds > 0) {
            delay(1000L)
            totalSeconds -= 1
        } else if (totalSeconds == 0) {
            // Trigger completion vibration
            triggerVibration(context)
            onComplete()
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
                ChamferTag(text = "15:30 TRANSITION INTERCEPTOR", colorAccent = BlueAccent)

                Text(
                    text = "20M NSDR & RECHARGE",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = White,
                    modifier = Modifier.padding(top = 10.dp)
                )

                Text(
                    text = "Non-Sleep Deep Rest breaks work fatigue & prevents afternoon procrastination.",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = Zinc400,
                    modifier = Modifier.padding(top = 6.dp, bottom = 40.dp)
                )

                // Breathing Ring Visualizer
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .scale(pulseScale),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Zinc950)
                            .border(2.dp, BlueAccent.copy(alpha = 0.6f), CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(170.dp)
                            .clip(CircleShape)
                            .background(Zinc900)
                            .border(1.dp, Zinc700, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        val mins = totalSeconds / 60
                        val secs = totalSeconds % 60
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = String.format("%02d:%02d", mins, secs),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Bold,
                                color = White
                            )
                            Text(
                                text = if (pulseScale > 1.0f) "EXHALE..." else "INHALE...",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                letterSpacing = 2.sp,
                                color = BlueAccent,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))

                // Tactical Control Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ChamferButton(
                        text = if (isRunning) "PAUSE" else "RESUME",
                        onClick = { isRunning = !isRunning },
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )
                    ChamferButton(
                        text = "RESET (20M)",
                        onClick = { totalSeconds = 20 * 60; isRunning = true },
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )
                    ChamferButton(
                        text = "DONE",
                        onClick = {
                            triggerVibration(context)
                            onComplete()
                        },
                        isPrimary = true,
                        colorAccent = EmeraldAccent,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "CLOSE INTERCEPTOR",
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

private fun triggerVibration(context: Context) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator?.vibrate(
                VibrationEffect.createWaveform(longArrayOf(0, 200, 100, 300), -1)
            )
        } else {
            @Suppress("DEPRECATION")
            val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            @Suppress("DEPRECATION")
            v?.vibrate(300L)
        }
    } catch (_: Exception) {}
}
