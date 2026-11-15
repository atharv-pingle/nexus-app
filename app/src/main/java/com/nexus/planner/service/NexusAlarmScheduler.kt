package com.nexus.planner.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

object NexusAlarmScheduler {
    private const val TAG = "NexusAlarmScheduler"
    const val ACTION_ALARM = "com.nexus.planner.ACTION_TRIGGER_ALARM"
    const val EXTRA_ITEM_ID = "extra_item_id"
    const val EXTRA_TITLE = "extra_title"
    const val EXTRA_TYPE = "extra_type" // TASK or HABIT

    fun scheduleTaskAlarm(
        context: Context,
        taskId: Long,
        title: String,
        scheduledDate: String, // "YYYY-MM-DD"
        reminderTime: String  // "HH:mm"
    ) {
        try {
            val date = LocalDate.parse(scheduledDate)
            val time = LocalTime.parse(reminderTime)
            val ldt = LocalDateTime.of(date, time)
            val epochMillis = ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

            if (epochMillis <= System.currentTimeMillis()) {
                Log.w(TAG, "Cannot schedule alarm in the past for task $taskId ($title)")
                return
            }

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, NexusAlarmReceiver::class.java).apply {
                action = ACTION_ALARM
                putExtra(EXTRA_ITEM_ID, taskId)
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_TYPE, "TASK")
            }

            val requestCode = (100_000 + taskId).toInt()
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    epochMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    epochMillis,
                    pendingIntent
                )
            }
            Log.d(TAG, "Scheduled alarm for task '$title' at $ldt (epoch $epochMillis)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule alarm for task $taskId", e)
        }
    }

    fun scheduleHabitAlarm(
        context: Context,
        habitId: Long,
        title: String,
        timeStr: String, // "HH:mm"
        activeDaysStr: String // "MON,TUE,WED,THU,FRI,SAT,SUN"
    ) {
        try {
            val time = LocalTime.parse(timeStr)
            var targetDate = LocalDate.now()
            val nowTime = LocalTime.now()

            // If time today has passed, start checking from tomorrow
            if (nowTime.isAfter(time)) {
                targetDate = targetDate.plusDays(1)
            }

            val activeDays = activeDaysStr.split(",").map { it.trim().uppercase() }.toSet()
            // Find next active day of week
            var count = 0
            while (count < 7) {
                val dayName = when (targetDate.dayOfWeek) {
                    DayOfWeek.MONDAY -> "MON"
                    DayOfWeek.TUESDAY -> "TUE"
                    DayOfWeek.WEDNESDAY -> "WED"
                    DayOfWeek.THURSDAY -> "THU"
                    DayOfWeek.FRIDAY -> "FRI"
                    DayOfWeek.SATURDAY -> "SAT"
                    DayOfWeek.SUNDAY -> "SUN"
                }
                if (activeDays.contains(dayName)) break
                targetDate = targetDate.plusDays(1)
                count++
            }

            val ldt = LocalDateTime.of(targetDate, time)
            val epochMillis = ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, NexusAlarmReceiver::class.java).apply {
                action = ACTION_ALARM
                putExtra(EXTRA_ITEM_ID, habitId)
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_TYPE, "HABIT")
            }

            val requestCode = (200_000 + habitId).toInt()
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    epochMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    epochMillis,
                    pendingIntent
                )
            }
            Log.d(TAG, "Scheduled alarm for habit '$title' at $ldt (epoch $epochMillis)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule habit alarm for $habitId", e)
        }
    }

    fun cancelTaskAlarm(context: Context, taskId: Long) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, NexusAlarmReceiver::class.java).apply {
                action = ACTION_ALARM
            }
            val requestCode = (100_000 + taskId).toInt()
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
            Log.d(TAG, "Cancelled alarm for task $taskId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cancel alarm for task $taskId", e)
        }
    }

    fun cancelHabitAlarm(context: Context, habitId: Long) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, NexusAlarmReceiver::class.java).apply {
                action = ACTION_ALARM
            }
            val requestCode = (200_000 + habitId).toInt()
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
            Log.d(TAG, "Cancelled alarm for habit $habitId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cancel alarm for habit $habitId", e)
        }
    }
}
