package com.taskalarm.app.alarm

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import com.taskalarm.app.R
import com.taskalarm.app.data.AppDatabase
import com.taskalarm.app.ui.screens.AlarmRingActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_NOT_STICKY

        when (action) {
            ACTION_START_ALARM -> {
                val taskId = intent.getLongExtra(AlarmReceiver.EXTRA_TASK_ID, -1L)
                val title = intent.getStringExtra(AlarmReceiver.EXTRA_TASK_TITLE) ?: "Vazifa eslatmasi"
                val desc = intent.getStringExtra(AlarmReceiver.EXTRA_TASK_DESC) ?: ""
                val vibrate = intent.getBooleanExtra(AlarmReceiver.EXTRA_VIBRATE, true)
                val ringtoneUri = intent.getStringExtra(AlarmReceiver.EXTRA_RINGTONE) ?: ""

                startForegroundAlarm(taskId, title, desc, vibrate, ringtoneUri)
            }
            ACTION_STOP_ALARM -> {
                stopAlarm()
            }
            ACTION_SNOOZE_ALARM -> {
                val taskId = intent.getLongExtra(AlarmReceiver.EXTRA_TASK_ID, -1L)
                handleSnooze(taskId)
            }
            ACTION_COMPLETE_ALARM -> {
                val taskId = intent.getLongExtra(AlarmReceiver.EXTRA_TASK_ID, -1L)
                handleComplete(taskId)
            }
        }

        return START_NOT_STICKY
    }

    private fun startForegroundAlarm(
        taskId: Long,
        title: String,
        desc: String,
        vibrate: Boolean,
        ringtoneUriString: String
    ) {
        val channelId = createNotificationChannel()

        // Ekran qulflangan bo'lsa ham ochiladigan to'liq ekran oynasi (Full-Screen Intent)
        val fullScreenIntent = Intent(this, AlarmRingActivity::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_TASK_ID, taskId)
            putExtra(AlarmReceiver.EXTRA_TASK_TITLE, title)
            putExtra(AlarmReceiver.EXTRA_TASK_DESC, desc)
            putExtra(AlarmReceiver.EXTRA_VIBRATE, vibrate)
            putExtra(AlarmReceiver.EXTRA_RINGTONE, ringtoneUriString)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            taskId.toInt(),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Tugmalar uchun Intentlar
        val completeIntent = Intent(this, AlarmService::class.java).apply {
            action = ACTION_COMPLETE_ALARM
            putExtra(AlarmReceiver.EXTRA_TASK_ID, taskId)
        }
        val completePendingIntent = PendingIntent.getService(
            this,
            (taskId * 10 + 1).toInt(),
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(this, AlarmService::class.java).apply {
            action = ACTION_SNOOZE_ALARM
            putExtra(AlarmReceiver.EXTRA_TASK_ID, taskId)
        }
        val snoozePendingIntent = PendingIntent.getService(
            this,
            (taskId * 10 + 2).toInt(),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, AlarmService::class.java).apply {
            action = ACTION_STOP_ALARM
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            (taskId * 10 + 3).toInt(),
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("⏰ $title")
            .setContentText(if (desc.isNotBlank()) desc else "Vaqt keldi! Vazifani bajaring.")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .addAction(android.R.drawable.checkbox_on_background, "Bajarildi", completePendingIntent)
            .addAction(android.R.drawable.ic_popup_sync, "5 daqiqa (Snooze)", snoozePendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "O'chirish", stopPendingIntent)
            .build()

        startForeground(NOTIFICATION_ID, notification)

        // Ovoz va vibratsiyani ishga tushirish
        playAlarmSound(ringtoneUriString)
        if (vibrate) {
            startVibration()
        }

        // Shuningdek, ekran yoqiq bo'lsa yoki ba'zi modellarda intentni to'g'ridan-to'g'ri chaqirish
        try {
            startActivity(fullScreenIntent)
        } catch (e: Exception) {
            Log.e("AlarmService", "Activity to'g'ridan-to'g'ri ochilmadi: ${e.message}")
        }
    }

    private fun playAlarmSound(ringtoneUriString: String) {
        try {
            mediaPlayer?.release()
            val uri = if (ringtoneUriString.isNotBlank()) {
                Uri.parse(ringtoneUriString)
            } else {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }

            mediaPlayer = MediaPlayer().apply {
                setDataSource(applicationContext, uri)
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setLegacyStreamType(AudioManager.STREAM_ALARM)
                    .build()
                setAudioAttributes(audioAttributes)
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e("AlarmService", "Ovoz chalishda xatolik: ${e.message}")
        }
    }

    private fun startVibration() {
        val pattern = longArrayOf(0, 800, 400, 800, 400)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, 0)
        }
    }

    private fun stopAlarm() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e("AlarmService", "MediaPlayer to'xtatish xatosi: ${e.message}")
        }
        vibrator?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun handleComplete(taskId: Long) {
        serviceScope.launch {
            if (taskId != -1L) {
                val db = AppDatabase.getInstance(applicationContext)
                val task = db.taskDao().getTaskById(taskId)
                if (task != null) {
                    db.taskDao().updateTask(task.copy(isCompleted = true, isEnabled = false))
                }
            }
        }
        stopAlarm()
    }

    private fun handleSnooze(taskId: Long) {
        serviceScope.launch {
            if (taskId != -1L) {
                val db = AppDatabase.getInstance(applicationContext)
                val task = db.taskDao().getTaskById(taskId)
                if (task != null) {
                    AlarmScheduler(applicationContext).scheduleSnooze(task, 5)
                }
            }
        }
        stopAlarm()
    }

    private fun createNotificationChannel(): String {
        val channelId = "TASK_ALARM_CHANNEL_V1"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Vazifalar Budilnigi Ovozli Xabari"
            val descriptionText = "Budilnik jiringlaganda ko'rinadigan yuqori ustunlikdagi xabar"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
                setSound(null, null) // Servis o'zi USAGE_ALARM bilan chaladi
                enableVibration(false) // Servis o'zi uzluksiz boshqaradi
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setBypassDnd(true) // "Bezovta qilinmasin" rejimini chetlab o'tish
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
        return channelId
    }

    override fun onDestroy() {
        stopAlarm()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val NOTIFICATION_ID = 1001
        const val ACTION_START_ALARM = "ACTION_START_ALARM"
        const val ACTION_STOP_ALARM = "ACTION_STOP_ALARM"
        const val ACTION_SNOOZE_ALARM = "ACTION_SNOOZE_ALARM"
        const val ACTION_COMPLETE_ALARM = "ACTION_COMPLETE_ALARM"
    }
}
