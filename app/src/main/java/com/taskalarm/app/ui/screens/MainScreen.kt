package com.taskalarm.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskalarm.app.data.RepeatMode
import com.taskalarm.app.data.Task
import com.taskalarm.app.ui.theme.*
import com.taskalarm.app.ui.viewmodel.TaskViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: TaskViewModel,
    onAddTaskClick: (Int) -> Unit,
    onEditTaskClick: (Task) -> Unit,
    onOpenPermissions: () -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Bugungi", "Har kunlik", "Tanlangan kunlar", "Kelgusi", "Bajarilgan")

    val todayTasks by viewModel.todayTasks.collectAsState()
    val dailyTasks by viewModel.dailyTasks.collectAsState()
    val customDayTasks by viewModel.customDayTasks.collectAsState()
    val upcomingTasks by viewModel.upcomingTasks.collectAsState()
    val completedTasks by viewModel.completedTasks.collectAsState()

    val currentTasks = when (selectedTabIndex) {
        0 -> todayTasks
        1 -> dailyTasks
        2 -> customDayTasks
        3 -> upcomingTasks
        else -> completedTasks
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(PrimaryIndigo),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Alarm,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Vazifalar Budilnigi",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenPermissions) {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = "Ruxsatlar",
                            tint = PrimaryIndigo
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onAddTaskClick(selectedTabIndex) },
                containerColor = PrimaryIndigo,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(60.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Yangi vazifa",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Scrollable Tab bo'limlari: Bugungi, Har kunlik, Tanlangan kunlar, Kelgusi, Bajarilgan
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                edgePadding = 12.dp,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = PrimaryIndigo
            ) {
                tabTitles.forEachIndexed { index, title ->
                    val badgeCount = when (index) {
                        0 -> todayTasks.size
                        1 -> dailyTasks.size
                        2 -> customDayTasks.size
                        3 -> upcomingTasks.size
                        else -> completedTasks.size
                    }
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = title,
                                    fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 14.sp
                                )
                                if (badgeCount > 0) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Badge(
                                        containerColor = if (selectedTabIndex == index) PrimaryIndigo else MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = if (selectedTabIndex == index) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    ) {
                                        Text(badgeCount.toString(), fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    )
                }
            }

            // Vazifalar ro'yxati yoki Bo'sh holat
            if (currentTasks.isEmpty()) {
                EmptyStateView(tabIndex = selectedTabIndex)
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(currentTasks, key = { it.id }) { task ->
                        TaskItemCard(
                            task = task,
                            nextTriggerMillis = viewModel.getNextTriggerMillis(task),
                            onToggleEnable = { isEnabled ->
                                viewModel.toggleTask(task, isEnabled)
                            },
                            onToggleComplete = { isCompleted ->
                                viewModel.toggleComplete(task, isCompleted)
                            },
                            onEdit = { onEditTaskClick(task) },
                            onDelete = { viewModel.deleteTask(task) }
                        )
                    }
                    // Floating button ostida bo'shliq qoldirish
                    item {
                        Spacer(modifier = Modifier.height(72.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun TaskItemCard(
    task: Task,
    nextTriggerMillis: Long,
    onToggleEnable: (Boolean) -> Unit,
    onToggleComplete: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val dateFormat = SimpleDateFormat("dd-MMMM", Locale("uz"))
    val taskCalendar = Calendar.getInstance().apply { timeInMillis = task.triggerTimeMillis }

    val formattedTime = timeFormat.format(taskCalendar.time)
    val formattedDate = when (task.repeatMode) {
        RepeatMode.DAILY -> "Har kunlik reja"
        RepeatMode.WEEKDAYS -> "Ish kunlari (Du–Ju)"
        RepeatMode.CUSTOM_DAYS -> "Tanlangan kunlar"
        RepeatMode.ONCE -> dateFormat.format(taskCalendar.time)
    }
    val remainingTimeText = remember(nextTriggerMillis) {
        TaskViewModel.formatRemainingTime(nextTriggerMillis)
    }

    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCompleted) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (task.isCompleted) 0.dp else 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // "Bajarildi" checkbox
                Checkbox(
                    checked = task.isCompleted,
                    onCheckedChange = onToggleComplete,
                    colors = CheckboxDefaults.colors(
                        checkedColor = SuccessGreen
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Vaqt va Sarlavha
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = formattedTime,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (task.isEnabled && !task.isCompleted) PrimaryIndigo else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = formattedDate,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = task.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                    )

                    if (task.description.isNotBlank()) {
                        Text(
                            text = task.description,
                            fontSize = 13.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Switch (yoqish/o'chirish)
                if (!task.isCompleted) {
                    Switch(
                        checked = task.isEnabled,
                        onCheckedChange = onToggleEnable
                    )
                }

                // Qo'shimcha menyu (uch nuqta)
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menyu")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Tahrirlash") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("O'chirish", color = DangerRed) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = DangerRed) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            // Tanlangan kunlar chiplari (agar CUSTOM_DAYS bo'lsa)
            if (task.repeatMode == RepeatMode.CUSTOM_DAYS && task.customDays.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                val selectedSet = task.customDays.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
                val todayNum = TaskViewModel.calendarDayToCustomDay(Calendar.getInstance().get(Calendar.DAY_OF_WEEK))
                val dayLabels = listOf(1 to "Du", 2 to "Se", 3 to "Ch", 4 to "Pa", 5 to "Ju", 6 to "Sha", 7 to "Ya")

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    dayLabels.forEach { (num, label) ->
                        val isSelected = selectedSet.contains(num)
                        val isToday = (num == todayNum)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    when {
                                        isSelected && isToday -> AccentOrange
                                        isSelected -> PrimaryIndigo
                                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    }
                                )
                                .padding(horizontal = 7.dp, vertical = 3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            // Pastki axborot: Qolgan vaqt va takrorlanish
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Qolgan vaqt
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = if (task.isEnabled && !task.isCompleted) AccentOrange else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (task.isCompleted) "Bajarilgan" else if (!task.isEnabled) "O'chirilgan" else remainingTimeText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (task.isEnabled && !task.isCompleted) AccentOrange else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Takrorlanish rejimi belgisi
                val repeatLabel = when (task.repeatMode) {
                    RepeatMode.ONCE -> "Bir martalik"
                    RepeatMode.DAILY -> "Har kuni"
                    RepeatMode.WEEKDAYS -> "Du–Ju"
                    RepeatMode.CUSTOM_DAYS -> "Tanlangan"
                }
                Text(
                    text = repeatLabel,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyStateView(tabIndex: Int) {
    val message = when (tabIndex) {
        0 -> "Bugun uchun vazifalar yo'q.\nYangi vazifa qo'shish uchun '+' tugmasini bosing!"
        1 -> "Har kunlik takrorlanuvchi vazifalar yo'q.\n'+' tugmasini bosib qo'shing!"
        2 -> "Tanlangan hafta kunlariga vazifalar qo'shilmagan.\n'+' tugmasini bosib hafta kunlarini belgilang!"
        3 -> "Kelgusi kunlarga vazifalar rejalashtirilmagan."
        else -> "Hozircha bajarilgan vazifalar yo'q."
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .background(PrimaryIndigo.copy(alpha = 0.08f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (tabIndex) {
                        1 -> Icons.Default.Repeat
                        2 -> Icons.Default.DateRange
                        4 -> Icons.Default.TaskAlt
                        else -> Icons.Default.EventNote
                    },
                    contentDescription = null,
                    modifier = Modifier.size(46.dp),
                    tint = PrimaryIndigo
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = message,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
