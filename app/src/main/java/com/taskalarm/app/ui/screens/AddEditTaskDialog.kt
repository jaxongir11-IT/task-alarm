package com.taskalarm.app.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.taskalarm.app.data.RepeatMode
import com.taskalarm.app.data.Task
import com.taskalarm.app.ui.theme.PrimaryIndigo
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AddEditTaskDialog(
    taskToEdit: Task? = null,
    onDismiss: () -> Unit,
    onSave: (Task) -> Unit,
    onSelectRingtone: () -> Unit,
    selectedRingtoneTitle: String = "Standart Budilnik Ovozi"
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(taskToEdit?.title ?: "") }
    var description by remember { mutableStateOf(taskToEdit?.description ?: "") }
    var repeatMode by remember { mutableStateOf(taskToEdit?.repeatMode ?: RepeatMode.ONCE) }
    var isVibrationEnabled by remember { mutableStateOf(taskToEdit?.isVibrationEnabled ?: true) }
    var ringtoneUri by remember { mutableStateOf(taskToEdit?.ringtoneUri ?: "") }

    val calendar = remember {
        Calendar.getInstance().apply {
            if (taskToEdit != null) {
                timeInMillis = taskToEdit.triggerTimeMillis
            } else {
                add(Calendar.HOUR_OF_DAY, 1)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }
        }
    }

    var selectedDateText by remember {
        mutableStateOf(SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(calendar.time))
    }
    var selectedTimeText by remember {
        mutableStateOf(SimpleDateFormat("HH:mm", Locale.getDefault()).format(calendar.time))
    }

    // Hafta kunlari (1=Du, ..., 7=Ya)
    val customDaysSet = remember {
        mutableStateListOf<Int>().apply {
            if (taskToEdit?.customDays?.isNotBlank() == true) {
                addAll(taskToEdit.customDays.split(",").mapNotNull { it.trim().toIntOrNull() })
            }
        }
    }

    val dayNames = listOf(
        1 to "Du",
        2 to "Se",
        3 to "Ch",
        4 to "Pa",
        5 to "Ju",
        6 to "Sha",
        7 to "Ya"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (taskToEdit == null) "Yangi Vazifa Qo'shish" else "Vazifani Tahrirlash",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Vazifa nomi
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Vazifa nomi *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Vazifa izohi
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Qisqa izoh (ixtiyoriy)") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Sana va Vaqt tanlash qatori
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Sana
                    OutlinedCard(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        calendar.set(Calendar.YEAR, year)
                                        calendar.set(Calendar.MONTH, month)
                                        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                        selectedDateText = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(calendar.time)
                                    },
                                    calendar.get(Calendar.YEAR),
                                    calendar.get(Calendar.MONTH),
                                    calendar.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = PrimaryIndigo)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Sana", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(selectedDateText, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            }
                        }
                    }

                    // Vaqt
                    OutlinedCard(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                TimePickerDialog(
                                    context,
                                    { _, hourOfDay, minute ->
                                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                                        calendar.set(Calendar.MINUTE, minute)
                                        calendar.set(Calendar.SECOND, 0)
                                        selectedTimeText = SimpleDateFormat("HH:mm", Locale.getDefault()).format(calendar.time)
                                    },
                                    calendar.get(Calendar.HOUR_OF_DAY),
                                    calendar.get(Calendar.MINUTE),
                                    true
                                ).show()
                            },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AccessTime, contentDescription = null, tint = PrimaryIndigo)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Vaqt", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(selectedTimeText, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Takrorlanish sozlamasi
                Text(
                    text = "Takrorlanish",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                val modes = listOf(
                    RepeatMode.ONCE to "Bir martalik",
                    RepeatMode.DAILY to "Har kuni",
                    RepeatMode.WEEKDAYS to "Ish kunlari (Du–Ju)",
                    RepeatMode.CUSTOM_DAYS to "Tanlangan kunlar"
                )

                modes.forEach { (mode, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { repeatMode = mode }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (repeatMode == mode),
                            onClick = { repeatMode = mode }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = label, fontSize = 14.sp)
                    }
                }

                // Agar CUSTOM_DAYS tanlansa, hafta kunlari
                if (repeatMode == RepeatMode.CUSTOM_DAYS) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        dayNames.forEach { (dayNum, dayLabel) ->
                            val isSelected = customDaysSet.contains(dayNum)
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) PrimaryIndigo else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable {
                                        if (isSelected) customDaysSet.remove(dayNum)
                                        else customDaysSet.add(dayNum)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = dayLabel,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Ringtone tanlash
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectRingtone() },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = PrimaryIndigo)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Budilnik musiqasi", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(selectedRingtoneTitle, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Vibratsiya switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Vibration, contentDescription = null, tint = PrimaryIndigo)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Vibratsiya", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                    Switch(
                        checked = isVibrationEnabled,
                        onCheckedChange = { isVibrationEnabled = it }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Tugmalar (Bekor qilish va Saqlash)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Bekor qilish")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                val finalCustomDays = customDaysSet.sorted().joinToString(",")
                                val newTask = (taskToEdit ?: Task(
                                    title = title.trim(),
                                    triggerTimeMillis = calendar.timeInMillis
                                )).copy(
                                    title = title.trim(),
                                    description = description.trim(),
                                    triggerTimeMillis = calendar.timeInMillis,
                                    repeatMode = repeatMode,
                                    customDays = finalCustomDays,
                                    isVibrationEnabled = isVibrationEnabled,
                                    ringtoneUri = ringtoneUri,
                                    isEnabled = true,
                                    isCompleted = false
                                )
                                onSave(newTask)
                            }
                        },
                        enabled = title.isNotBlank(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Saqlash")
                    }
                }
            }
        }
    }
}
