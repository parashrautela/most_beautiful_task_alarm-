package com.example.myapplication

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskTitle = intent.getStringExtra("TASK_TITLE") ?: "Task Alarm"
        val taskDesc = intent.getStringExtra("TASK_DESC") ?: "Time to get things done!"

        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra("TASK_TITLE", taskTitle)
            putExtra("TASK_DESC", taskDesc)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}
