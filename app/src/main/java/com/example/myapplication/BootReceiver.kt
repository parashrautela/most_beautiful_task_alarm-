package com.example.myapplication

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import java.time.LocalDateTime

/**
 * Re-schedules all saved alarms after device reboot.
 * AlarmManager alarms are cleared when the device restarts,
 * so this receiver restores them from SharedPreferences.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        Log.d("BootReceiver", "Device booted — rescheduling alarms")

        val tasks = TaskStorage.getTasks(context)
        val now = LocalDateTime.now()

        for (task in tasks) {
            try {
                val alarmTime = LocalDateTime.parse(task.dateTime)
                if (alarmTime.isAfter(now)) {
                    AlarmScheduler.scheduleAlarm(
                        context = context,
                        time = alarmTime,
                        title = task.title,
                        description = task.description,
                        taskId = task.id
                    )
                    Log.d("BootReceiver", "Rescheduled alarm: ${task.title} at $alarmTime")
                }
            } catch (e: Exception) {
                Log.e("BootReceiver", "Failed to reschedule alarm for task ${task.id}", e)
            }
        }
    }
}
