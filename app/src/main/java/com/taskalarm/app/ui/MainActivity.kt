package com.taskalarm.app.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.taskalarm.app.data.RepeatMode
import com.taskalarm.app.data.Task
import com.taskalarm.app.ui.screens.AddEditTaskDialog
import com.taskalarm.app.ui.screens.MainScreen
import com.taskalarm.app.ui.screens.PermissionsDialog
import com.taskalarm.app.ui.theme.TaskAlarmTheme
import com.taskalarm.app.ui.viewmodel.TaskViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: TaskViewModel by viewModels()

    private var selectedRingtoneUri by mutableStateOf<Uri?>(null)
    private var selectedRingtoneTitle by mutableStateOf("Standart Budilnik")

    // Ringtone tanlash intent natijasi
    private val ringtonePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            selectedRingtoneUri = uri
            if (uri != null) {
                val ringtone = RingtoneManager.getRingtone(this, uri)
                selectedRingtoneTitle = ringtone.getTitle(this) ?: "Tanlangan Ovoz"
            } else {
                selectedRingtoneTitle = "Ovozsiz"
            }
        }
    }

    // Android 13+ Notification ruxsat so'rovi
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Ruxsat berildi yoki rad etildi
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Birinchi kirishni tekshirish
        val prefs = getSharedPreferences("task_alarm_prefs", Context.MODE_PRIVATE)
        val isFirstRun = prefs.getBoolean("is_first_run", true)

        setContent {
            TaskAlarmTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var showPermissionsDialog by remember { mutableStateOf(isFirstRun) }
                    var showAddEditDialog by remember { mutableStateOf(false) }
                    var taskToEdit by remember { mutableStateOf<Task?>(null) }
                    var defaultRepeatMode by remember { mutableStateOf(RepeatMode.ONCE) }

                    MainScreen(
                        viewModel = viewModel,
                        onAddTaskClick = { tabIndex ->
                            taskToEdit = null
                            defaultRepeatMode = when (tabIndex) {
                                1 -> RepeatMode.DAILY
                                2 -> RepeatMode.CUSTOM_DAYS
                                else -> RepeatMode.ONCE
                            }
                            selectedRingtoneUri = null
                            selectedRingtoneTitle = "Standart Budilnik Ovozi"
                            showAddEditDialog = true
                        },
                        onEditTaskClick = { task ->
                            taskToEdit = task
                            if (task.ringtoneUri.isNotBlank()) {
                                try {
                                    val uri = Uri.parse(task.ringtoneUri)
                                    selectedRingtoneUri = uri
                                    val ringtone = RingtoneManager.getRingtone(this@MainActivity, uri)
                                    selectedRingtoneTitle = ringtone.getTitle(this@MainActivity) ?: "Tanlangan Ovoz"
                                } catch (e: Exception) {
                                    selectedRingtoneTitle = "Standart Budilnik Ovozi"
                                }
                            } else {
                                selectedRingtoneTitle = "Standart Budilnik Ovozi"
                            }
                            showAddEditDialog = true
                        },
                        onOpenPermissions = {
                            showPermissionsDialog = true
                        }
                    )

                    // Ruxsatlar oynasi
                    if (showPermissionsDialog) {
                        PermissionsDialog(
                            onDismiss = {
                                showPermissionsDialog = false
                                prefs.edit().putBoolean("is_first_run", false).apply()
                            },
                            onRequestNotificationPermission = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            }
                        )
                    }

                    // Vazifa qo'shish / tahrirlash dialogi
                    if (showAddEditDialog) {
                        AddEditTaskDialog(
                            taskToEdit = taskToEdit,
                            defaultRepeatMode = defaultRepeatMode,
                            onDismiss = { showAddEditDialog = false },
                            onSave = { task ->
                                val finalTask = if (selectedRingtoneUri != null) {
                                    task.copy(ringtoneUri = selectedRingtoneUri.toString())
                                } else {
                                    task
                                }
                                viewModel.saveTask(finalTask)
                                showAddEditDialog = false
                            },
                            onSelectRingtone = {
                                val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Budilnik ovozini tanlang")
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, selectedRingtoneUri)
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                                }
                                ringtonePickerLauncher.launch(intent)
                            },
                            selectedRingtoneTitle = selectedRingtoneTitle
                        )
                    }
                }
            }
        }
    }
}
