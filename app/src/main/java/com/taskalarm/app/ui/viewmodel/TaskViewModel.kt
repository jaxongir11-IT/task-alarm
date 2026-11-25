package com.taskalarm.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.taskalarm.app.alarm.AlarmScheduler
import com.taskalarm.app.data.AppDatabase
import com.taskalarm.app.data.RepeatMode
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
        val now = Calendar.getInstance()
        val currentYear = now.get(Calendar.YEAR)
        val currentDayOfYear = now.get(Calendar.DAY_OF_YEAR)
        val currentDayNum = calendarDayToCustomDay(now.get(Calendar.DAY_OF_WEEK))

        tasks.filter { task ->
            if (task.isCompleted) return@filter false
            when (task.repeatMode) {
                RepeatMode.DAILY -> true
                RepeatMode.WEEKDAYS -> currentDayNum in 1..5
                RepeatMode.CUSTOM_DAYS -> {
                    val selected = task.customDays.split(",").mapNotNull { it.trim().toIntOrNull() }
                    selected.contains(currentDayNum)
                }
                RepeatMode.ONCE -> {
                    val taskCal = Calendar.getInstance().apply { timeInMillis = task.triggerTimeMillis }
                    taskCal.get(Calendar.YEAR) == currentYear && taskCal.get(Calendar.DAY_OF_YEAR) == currentDayOfYear
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dailyTasks: StateFlow<List<Task>> = allTasks.map { tasks ->
        tasks.filter { !it.isCompleted && it.repeatMode == RepeatMode.DAILY }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customDayTasks: StateFlow<List<Task>> = allTasks.map { tasks ->
        tasks.filter { !it.isCompleted && it.repeatMode == RepeatMode.CUSTOM_DAYS }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val upcomingTasks: StateFlow<List<Task>> = allTasks.map { tasks ->
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentDayOfYear = calendar.get(Calendar.DAY_OF_YEAR)

        tasks.filter { task ->
            if (task.isCompleted || task.repeatMode != RepeatMode.ONCE) return@filter false
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
            val nextTrigger = if (task.repeatMode != RepeatMode.ONCE) {
                alarmScheduler.calculateNextTriggerTime(task)
            } else {
                task.triggerTimeMillis
            }
            val preparedTask = task.copy(triggerTimeMillis = nextTrigger)

            if (preparedTask.id == 0L) {
                val newId = taskDao.insertTask(preparedTask)
                val insertedTask = preparedTask.copy(id = newId)
                if (insertedTask.isEnabled && !insertedTask.isCompleted) {
                    alarmScheduler.schedule(insertedTask)
                }
            } else {
                taskDao.updateTask(preparedTask)
                if (preparedTask.isEnabled && !preparedTask.isCompleted) {
                    alarmScheduler.schedule(preparedTask)
                } else {
                    alarmScheduler.cancel(preparedTask)
                }
            }
        }
    }

    fun toggleTask(task: Task, isEnabled: Boolean) {
        viewModelScope.launch {
            val nextTrigger = if (isEnabled && task.repeatMode != RepeatMode.ONCE) {
                alarmScheduler.calculateNextTriggerTime(task)
            } else {
                task.triggerTimeMillis
            }
            val updatedTask = task.copy(isEnabled = isEnabled, triggerTimeMillis = nextTrigger)
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

    fun getNextTriggerMillis(task: Task): Long {
        return alarmScheduler.calculateNextTriggerTime(task)
    }

    companion object {
        fun calendarDayToCustomDay(calendarDay: Int): Int {
            return when (calendarDay) {
                Calendar.MONDAY -> 1
                Calendar.TUESDAY -> 2
                Calendar.WEDNESDAY -> 3
                Calendar.THURSDAY -> 4
                Calendar.FRIDAY -> 5
                Calendar.SATURDAY -> 6
                else -> 7 // SUNDAY
            }
        }

        fun formatRemainingTime(triggerTimeMillis: Long): String {
            val now = System.currentTimeMillis()
            val diff = triggerTimeMillis - now
            if (diff <= 0) return "Vaqti o'tgan"

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
