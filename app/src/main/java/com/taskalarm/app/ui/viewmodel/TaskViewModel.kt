package com.taskalarm.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.taskalarm.app.alarm.AlarmScheduler
import com.taskalarm.app.data.AppDatabase
import com.taskalarm.app.data.Task
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val taskDao = db.taskDao()
    private val alarmScheduler = AlarmScheduler(application)

    val allTasks: StateFlow<List<Task>> = taskDao.getAllTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayTasks: StateFlow<List<Task>> = allTasks.map { tasks ->
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentDayOfYear = calendar.get(Calendar.DAY_OF_YEAR)

        tasks.filter { task ->
            if (task.isCompleted) return@filter false
            val taskCal = Calendar.getInstance().apply { timeInMillis = task.triggerTimeMillis }
            taskCal.get(Calendar.YEAR) == currentYear && taskCal.get(Calendar.DAY_OF_YEAR) == currentDayOfYear
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val upcomingTasks: StateFlow<List<Task>> = allTasks.map { tasks ->
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentDayOfYear = calendar.get(Calendar.DAY_OF_YEAR)

        tasks.filter { task ->
            if (task.isCompleted) return@filter false
            val taskCal = Calendar.getInstance().apply { timeInMillis = task.triggerTimeMillis }
            taskCal.get(Calendar.YEAR) > currentYear ||
                    (taskCal.get(Calendar.YEAR) == currentYear && taskCal.get(Calendar.DAY_OF_YEAR) > currentDayOfYear)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val completedTasks: StateFlow<List<Task>> = allTasks.map { tasks ->
        tasks.filter { it.isCompleted }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveTask(task: Task) {
        viewModelScope.launch {
            if (task.id == 0L) {
                val newId = taskDao.insertTask(task)
                val insertedTask = task.copy(id = newId)
                if (insertedTask.isEnabled && !insertedTask.isCompleted) {
                    alarmScheduler.schedule(insertedTask)
                }
            } else {
                taskDao.updateTask(task)
                if (task.isEnabled && !task.isCompleted) {
                    alarmScheduler.schedule(task)
                } else {
                    alarmScheduler.cancel(task)
                }
            }
        }
    }

    fun toggleTask(task: Task, isEnabled: Boolean) {
        viewModelScope.launch {
            val updatedTask = task.copy(isEnabled = isEnabled)
            taskDao.updateTask(updatedTask)
            if (isEnabled && !task.isCompleted) {
                alarmScheduler.schedule(updatedTask)
            } else {
                alarmScheduler.cancel(task)
            }
        }
    }

    fun toggleComplete(task: Task, isCompleted: Boolean) {
        viewModelScope.launch {
            val updatedTask = task.copy(isCompleted = isCompleted, isEnabled = !isCompleted)
            taskDao.updateTask(updatedTask)
            if (isCompleted) {
                alarmScheduler.cancel(task)
            } else if (updatedTask.isEnabled) {
                alarmScheduler.schedule(updatedTask)
            }
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            alarmScheduler.cancel(task)
            taskDao.deleteTask(task)
        }
    }

    companion object {
        fun formatRemainingTime(triggerTimeMillis: Long): String {
            val now = System.currentTimeMillis()
            val diff = triggerTimeMillis - now
            if (diff <= 0) return "Vaqti yetib kelgan"

            val minutes = (diff / (1000 * 60)) % 60
            val hours = (diff / (1000 * 60 * 60)) % 24
            val days = diff / (1000 * 60 * 60 * 24)

            return when {
                days > 0 -> "$days kun $hours soatdan keyin"
                hours > 0 -> "$hours soat $minutes daqiqadan keyin"
                else -> "$minutes daqiqadan keyin"
            }
        }
    }
}
