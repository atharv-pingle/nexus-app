package com.nexus.planner.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.Vibrator
import android.os.VibrationEffect
import androidx.core.app.NotificationCompat
import com.nexus.planner.MainActivity
import com.nexus.planner.R

class NexusAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "nexus_alarms_channel"
        const val CHANNEL_NAME = "Nexus Alarms & Reminders"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(NexusAlarmScheduler.EXTRA_TITLE) ?: "Scheduled Item"
        val itemId = intent.getLongExtra(NexusAlarmScheduler.EXTRA_ITEM_ID, 0L)
        val type = intent.getStringExtra(NexusAlarmScheduler.EXTRA_TYPE) ?: "TASK"

        showAlarmNotification(context, title, itemId, type)
        triggerVibration(context)
    }

    private fun showAlarmNotification(context: Context, title: String, itemId: Long, type: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High-priority alarms and reminders for tasks and habits"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 250, 500)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("alarm_item_id", itemId)
            putExtra("alarm_type", type)
            putExtra("alarm_title", title)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            itemId.toInt(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val contentText = if (type == "HABIT") "Habit commitment is due now" else "Task sprint is scheduled to start"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("⚡ NEXUS // $title")
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setSound(defaultSound)
            .setVibrate(longArrayOf(0, 500, 250, 500))
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(itemId.toInt(), notification)
    }

    private fun triggerVibration(context: Context) {
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 400, 200, 400), -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 400, 200, 400), -1)
            }
        } catch (_: Exception) {}
    }
}
