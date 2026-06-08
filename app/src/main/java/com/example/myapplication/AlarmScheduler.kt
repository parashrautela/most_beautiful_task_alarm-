package com.example.myapplication

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.os.Build
import android.util.Log
import java.time.LocalDateTime
import java.time.ZoneId

object AlarmScheduler {
    private const val TAG = "AlarmScheduler"

    fun scheduleAlarm(context: Context, time: LocalDateTime, title: String, description: String, taskId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Check for exact alarm permission on Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                // Try opening the exact alarm settings for the user
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Cannot open exact alarm settings", e)
                }
                // Fall back to inexact alarm so the alarm still fires (within ~10 min window)
                scheduleInexactAlarm(context, alarmManager, time, title, description, taskId)
                return
            }
        }

        val pendingIntent = buildPendingIntent(context, title, description, taskId)
        val triggerTime = time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
            Log.d(TAG, "Exact alarm scheduled for $time (taskId=$taskId)")
        } catch (e: SecurityException) {
            // Exact alarm permission was revoked between the check and the call
            Log.w(TAG, "SecurityException scheduling exact alarm, falling back to inexact", e)
            scheduleInexactAlarm(context, alarmManager, time, title, description, taskId)
        }
    }

    /**
     * Fallback: schedule an inexact alarm so the alarm still fires
     * even if exact alarm permission is not granted (within ~10 min window).
     */
    private fun scheduleInexactAlarm(
        context: Context,
        alarmManager: AlarmManager,
        time: LocalDateTime,
        title: String,
        description: String,
        taskId: String
    ) {
        val pendingIntent = buildPendingIntent(context, title, description, taskId)
        val triggerTime = time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerTime,
            pendingIntent
        )
        Log.d(TAG, "Inexact alarm scheduled for $time (taskId=$taskId)")
    }

    private fun buildPendingIntent(context: Context, title: String, description: String, taskId: String): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("TASK_TITLE", title)
            putExtra("TASK_DESC", description)
            putExtra("TASK_ID", taskId)
        }

        return PendingIntent.getBroadcast(
            context,
            taskId.hashCode(), // Use taskId for unique, stable PendingIntent per alarm
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun cancelAlarm(context: Context, taskId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        Log.d(TAG, "Alarm cancelled for taskId=$taskId")
    }
}
