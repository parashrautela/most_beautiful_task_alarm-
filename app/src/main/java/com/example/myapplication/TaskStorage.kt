package com.example.myapplication

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalTime

data class TaskAlarm(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val dateTime: String, // ISO-8601 format
    val priority: Int, // 0=Important, 1=Critical, 2=Flexible
    val reschedulesRemaining: Int? = 2,
    val snoozeCount: Int = 0
)

data class DroppedTaskInfo(
    val title: String,
    val count: Int
)

data class TaskStats(
    val completedCount: Int,
    val rescheduledCount: Int,
    val droppedCount: Int,
    val avgDelayHours: Float,
    val delayRecordsCount: Int,
    val activeHours: List<Int>, // 9 buckets: 6AM, 8AM, 10AM, 12PM, 2PM, 4PM, 6PM, 8PM, 10PM
    val droppedTasks: List<DroppedTaskInfo>
)

object TaskStorage {
    private const val PREFS_NAME = "task_alarms"
    private const val TASKS_KEY = "tasks"
    private const val STATS_KEY = "stats"
    private val gson = Gson()

    fun saveTask(context: Context, task: TaskAlarm) {
        val tasks = getTasks(context).toMutableList()
        tasks.add(task)
        val json = gson.toJson(tasks)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(TASKS_KEY, json)
            .apply()
    }

    fun updateTask(context: Context, updatedTask: TaskAlarm) {
        val tasks = getTasks(context).map {
            if (it.id == updatedTask.id) updatedTask else it
        }
        val json = gson.toJson(tasks)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(TASKS_KEY, json)
            .apply()
    }

    fun deleteTask(context: Context, id: String) {
        val tasks = getTasks(context).filter { it.id != id }
        val json = gson.toJson(tasks)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(TASKS_KEY, json)
            .apply()
    }

    fun getTasks(context: Context): List<TaskAlarm> {
        val json = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(TASKS_KEY, null) ?: return emptyList()
        val type = object : TypeToken<List<TaskAlarm>>() {}.type
        return gson.fromJson(json, type)
    }

    // ─── Stats Tracking ──────────────────────────────────────────────────────

    fun getStats(context: Context): TaskStats {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(STATS_KEY, null)
        if (json == null) {
            // Seed initial statistics to match Figma design
            val defaultStats = TaskStats(
                completedCount = 2,
                rescheduledCount = 0,
                droppedCount = 6,
                avgDelayHours = 1.5f,
                delayRecordsCount = 1,
                activeHours = listOf(2, 5, 8, 6, 10, 7, 4, 3, 1),
                droppedTasks = listOf(
                    DroppedTaskInfo("Review project proposal", 3),
                    DroppedTaskInfo("Update documentation", 2),
                    DroppedTaskInfo("Team standup meeting", 1)
                )
            )
            saveStats(context, defaultStats)
            return defaultStats
        }
        val type = object : TypeToken<TaskStats>() {}.type
        return gson.fromJson(json, type)
    }

    private fun saveStats(context: Context, stats: TaskStats) {
        val json = gson.toJson(stats)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(STATS_KEY, json)
            .apply()
    }

    fun logCompletion(context: Context, completionTime: LocalTime = LocalTime.now(), delayHours: Float = 0f) {
        val currentStats = getStats(context)
        val newCompletedCount = currentStats.completedCount + 1
        
        // Calculate new average delay
        val newRecordsCount = currentStats.delayRecordsCount + 1
        val newAvgDelay = ((currentStats.avgDelayHours * currentStats.delayRecordsCount) + delayHours) / newRecordsCount

        // Update active hours bucket (nearest hour)
        val hour = completionTime.hour
        val bucketIndex = when {
            hour <= 6 -> 0  // 6AM
            hour <= 8 -> 1  // 8AM
            hour <= 10 -> 2 // 10AM
            hour <= 12 -> 3 // 12PM
            hour <= 14 -> 4 // 2PM
            hour <= 16 -> 5 // 4PM
            hour <= 18 -> 6 // 6PM
            hour <= 20 -> 7 // 8PM
            else -> 8       // 10PM
        }
        val newActiveHours = currentStats.activeHours.toMutableList()
        newActiveHours[bucketIndex] = newActiveHours[bucketIndex] + 1

        val updatedStats = currentStats.copy(
            completedCount = newCompletedCount,
            avgDelayHours = newAvgDelay,
            delayRecordsCount = newRecordsCount,
            activeHours = newActiveHours
        )
        saveStats(context, updatedStats)
    }

    fun logReschedule(context: Context) {
        val currentStats = getStats(context)
        val updatedStats = currentStats.copy(
            rescheduledCount = currentStats.rescheduledCount + 1
        )
        saveStats(context, updatedStats)
    }

    fun logDrop(context: Context, taskTitle: String) {
        val currentStats = getStats(context)
        val newDroppedCount = currentStats.droppedCount + 1

        // Update or insert task title in dropped list
        val droppedList = currentStats.droppedTasks.toMutableList()
        val index = droppedList.indexOfFirst { it.title.equals(taskTitle, ignoreCase = true) }
        if (index != -1) {
            droppedList[index] = droppedList[index].copy(count = droppedList[index].count + 1)
        } else {
            droppedList.add(DroppedTaskInfo(taskTitle, 1))
        }
        // Sort by count descending
        val sortedDroppedList = droppedList.sortedByDescending { it.count }

        val updatedStats = currentStats.copy(
            droppedCount = newDroppedCount,
            droppedTasks = sortedDroppedList
        )
        saveStats(context, updatedStats)
    }
}
