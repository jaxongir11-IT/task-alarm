package com.taskalarm.app.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.taskalarm.app.data.RepeatMode
import com.taskalarm.app.data.Task
import com.taskalarm.app.ui.MainActivity
import java.util.Calendar

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /**
     * Vazifa uchun haqiqiy budilnikni rejalashtirish (setAlarmClock yordamida)
     */
    fun schedule(task: Task) {
        if (!task.isEnabled || task.isCompleted) {
            cancel(task)
            return
        }

        val triggerTime = calculateNextTriggerTime(task)
        if (triggerTime <= System.currentTimeMillis()) {
            return
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_ALARM_TRIGGER
            putExtra(AlarmReceiver.EXTRA_TASK_ID, task.id)
            putExtra(AlarmReceiver.EXTRA_TASK_TITLE, task.title)
            putExtra(AlarmReceiver.EXTRA_TASK_DESC, task.description)
            putExtra(AlarmReceiver.EXTRA_VIBRATE, task.isVibrationEnabled)
            putExtra(AlarmReceiver.EXTRA_RINGTONE, task.ringtoneUri)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Foydalanuvchi status bardagi budilnik ikonkasiga bossa MainActivity ochilishi uchun
        val showIntent = Intent(context, MainActivity::class.java)
        val showPendingIntent = PendingIntent.getActivity(
            context,
            task.id.toInt(),
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTime, showPendingIntent)

        try {
            // setAlarmClock - Android tizimidagi eng yuqori darajadagi budilnik usuli.
            // Bu Doze (quvvat tejash) rejimini yorib o'tadi va telefonni 100% uyg'otadi.
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
            Log.d("AlarmScheduler", "Alarm rejalashtirildi: ${task.title}, vaqt: $triggerTime")
        } catch (e: SecurityException) {
            Log.e("AlarmScheduler", "Aniq budilnik ruxsati yo'q: ${e.message}")
        }
    }

    /**
     * 5 daqiqaga kechiktirish (Snooze)
     */
    fun scheduleSnooze(task: Task, snoozeMinutes: Int = 5) {
        val snoozeTime = System.currentTimeMillis() + (snoozeMinutes * 60 * 1000)

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_ALARM_TRIGGER
            putExtra(AlarmReceiver.EXTRA_TASK_ID, task.id)
            putExtra(AlarmReceiver.EXTRA_TASK_TITLE, task.title)
            putExtra(AlarmReceiver.EXTRA_TASK_DESC, task.description)
            putExtra(AlarmReceiver.EXTRA_VIBRATE, task.isVibrationEnabled)
            putExtra(AlarmReceiver.EXTRA_RINGTONE, task.ringtoneUri)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val showIntent = Intent(context, MainActivity::class.java)
        val showPendingIntent = PendingIntent.getActivity(
            context,
            task.id.toInt(),
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmClockInfo = AlarmManager.AlarmClockInfo(snoozeTime, showPendingIntent)
        try {
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
        } catch (e: Exception) {
            Log.e("AlarmScheduler", "Snooze xatosi: ${e.message}")
        }
    }

    /**
     * Budilnikni bekor qilish
     */
    fun cancel(task: Task) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_ALARM_TRIGGER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    /**
     * Keyingi chalinish vaqtini takrorlanish rejimiga ko'ra hisoblash
     */
    fun calculateNextTriggerTime(task: Task): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            timeInMillis = task.triggerTimeMillis
        }

        val hour = target.get(Calendar.HOUR_OF_DAY)
        val minute = target.get(Calendar.MINUTE)

        when (task.repeatMode) {
            RepeatMode.ONCE -> {
                // Agar belgilangan vaqt o'tmishda bo'lsa, xuddi shu vaqt saqlanadi (chalinmaydi)
                return task.triggerTimeMillis
            }
            RepeatMode.DAILY -> {
                val nextCalendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                if (nextCalendar.timeInMillis <= now.timeInMillis) {
                    nextCalendar.add(Calendar.DAY_OF_YEAR, 1)
                }
                return nextCalendar.timeInMillis
            }
            RepeatMode.WEEKDAYS -> {
                // Du-Ju (Dushanba=2, ..., Juma=6)
                var nextCalendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                if (nextCalendar.timeInMillis <= now.timeInMillis) {
                    nextCalendar.add(Calendar.DAY_OF_YEAR, 1)
                }
                while (nextCalendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY ||
                    nextCalendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                    nextCalendar.add(Calendar.DAY_OF_YEAR, 1)
                }
                return nextCalendar.timeInMillis
            }
            RepeatMode.CUSTOM_DAYS -> {
                if (task.customDays.isBlank()) {
                    return task.triggerTimeMillis
                }
                // customDays formati: "1,2,5" (1=Du, 2=Se, 3=Ch, 4=Pa, 5=Ju, 6=Sha, 7=Ya)
                val selectedDays = task.customDays.split(",").mapNotNull { it.trim().toIntOrNull() }
                if (selectedDays.isEmpty()) return task.triggerTimeMillis

                // Calendar.DAY_OF_WEEK: SUNDAY=1, MONDAY=2, ... SATURDAY=7
                // Bizning formatimiz: 1=Du(2), 2=Se(3), 3=Ch(4), 4=Pa(5), 5=Ju(6), 6=Sha(7), 7=Ya(1)
                val dayOfWeekMap = mapOf(
                    1 to Calendar.MONDAY,
                    2 to Calendar.TUESDAY,
                    3 to Calendar.WEDNESDAY,
                    4 to Calendar.THURSDAY,
                    5 to Calendar.FRIDAY,
                    6 to Calendar.SATURDAY,
                    7 to Calendar.SUNDAY
                )
                val calendarTargetDays = selectedDays.mapNotNull { dayOfWeekMap[it] }.toSet()

                for (dayOffset in 0..7) {
                    val candidate = Calendar.getInstance().apply {
                        add(Calendar.DAY_OF_YEAR, dayOffset)
                        set(Calendar.HOUR_OF_DAY, hour)
                        set(Calendar.MINUTE, minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    if (candidate.timeInMillis > now.timeInMillis &&
                        calendarTargetDays.contains(candidate.get(Calendar.DAY_OF_WEEK))) {
                        return candidate.timeInMillis
                    }
                }
                return task.triggerTimeMillis
            }
        }
    }
}
