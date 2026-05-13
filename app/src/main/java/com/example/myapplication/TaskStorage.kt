package com.example.myapplication

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class TaskAlarm(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val dateTime: String, // ISO-8601 format
    val priority: Int // 0=Important, 1=Critical, 2=Flexible
)

object TaskStorage {
    private const val PREFS_NAME = "task_alarms"
    private const val TASKS_KEY = "tasks"
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

    fun getTasks(context: Context): List<TaskAlarm> {
        val json = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(TASKS_KEY, null) ?: return emptyList()
        val type = object : TypeToken<List<TaskAlarm>>() {}.type
        return gson.fromJson(json, type)
    }
}
