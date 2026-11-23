package com.taskalarm.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.util.Log

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        val title = intent.getStringExtra(EXTRA_TASK_TITLE) ?: "Vazifa Vaqti Bo'ldi!"
        val desc = intent.getStringExtra(EXTRA_TASK_DESC) ?: ""
        val vibrate = intent.getBooleanExtra(EXTRA_VIBRATE, true)
        val ringtone = intent.getStringExtra(EXTRA_RINGTONE) ?: ""

        Log.d("AlarmReceiver", "Budilnik qabul qilindi: ID=$taskId, Sarlavha=$title")

        // Telefonni darhol uyg'otish uchun WakeLock
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "TaskAlarm:AlarmReceiverWakeLock"
        )
        wakeLock.acquire(60 * 1000L) // 60 soniya

        // Budilnik uzluksiz chalinishi va notifikatsiya chiqishi uchun Service ishga tushiriladi
        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            action = AlarmService.ACTION_START_ALARM
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_TASK_TITLE, title)
            putExtra(EXTRA_TASK_DESC, desc)
            putExtra(EXTRA_VIBRATE, vibrate)
            putExtra(EXTRA_RINGTONE, ringtone)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }

    companion object {
        const val ACTION_ALARM_TRIGGER = "com.taskalarm.app.ACTION_ALARM_TRIGGER"
        const val EXTRA_TASK_ID = "EXTRA_TASK_ID"
        const val EXTRA_TASK_TITLE = "EXTRA_TASK_TITLE"
        const val EXTRA_TASK_DESC = "EXTRA_TASK_DESC"
        const val EXTRA_VIBRATE = "EXTRA_VIBRATE"
        const val EXTRA_RINGTONE = "EXTRA_RINGTONE"
    }
}
