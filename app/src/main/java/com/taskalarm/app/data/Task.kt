package com.taskalarm.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val triggerTimeMillis: Long,
    val repeatMode: RepeatMode = RepeatMode.ONCE,
    val customDays: String = "", // Vergul bilan ajratilgan kunlar: masalan "1,3,5" (1=Dushanba, ..., 7=Yakshanba)
    val isVibrationEnabled: Boolean = true,
    val ringtoneUri: String = "", // Tizim budilnik ovozi Uri string ko'rinishida
    val isEnabled: Boolean = true, // Budilnik yoqilgan yoki o'chirilgan (toggle)
    val isCompleted: Boolean = false, // Bajarilgan deb belgilangan
    val createdAt: Long = System.currentTimeMillis()
)
