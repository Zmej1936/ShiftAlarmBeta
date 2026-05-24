package com.example.shiftalarm.ui.screens

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import androidx.navigation.NavController
import com.example.shiftalarm.data.Alarm
import com.example.shiftalarm.data.ShiftType
import com.example.shiftalarm.settingsDataStore
import com.example.shiftalarm.utils.ShiftCalculator
import com.example.shiftalarm.viewmodel.AlarmViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.datastore.preferences.core.stringPreferencesKey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmListScreen(
    navController: NavController,
    viewModel: AlarmViewModel
) {
    val alarms by viewModel.alarms.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var startDate by remember { mutableStateOf("2026-05-20") }
    var isLoading by remember { mutableStateOf(true) }

    // Слушаем широковещательное сообщение о срабатывании будильника (для обновления UI)
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                scope.launch {
                    try {
                        val prefs = context.settingsDataStore.data.first()
                        val newStartDate = prefs[stringPreferencesKey("start_date")] ?: "2026-05-20"
                        ShiftCalculator.setStartDate(newStartDate)
                        startDate = newStartDate
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
        val intentFilter = IntentFilter("ALARM_CHANGED")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, intentFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, intentFilter)
        }
        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    LaunchedEffect(Unit) {
        try {
            val prefs = context.settingsDataStore.data.first()
            startDate = prefs[stringPreferencesKey("start_date")] ?: "2026-05-20"
            ShiftCalculator.setStartDate(startDate)
        } catch (e: Exception) {
            e.printStackTrace()
            ShiftCalculator.setStartDate("2026-05-20")
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Сменный будильник") },
                actions = {
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(Icons.Default.Settings, contentDescription = "Настройки")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("edit_alarm/-1") }) {
                Icon(Icons.Default.Add, contentDescription = "Добавить будильник")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (alarms.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Нет будильников",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Нажмите + для добавления",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(alarms) { alarm ->
                        AlarmCard(
                            alarm = alarm,
                            onDelete = { viewModel.deleteAlarm(alarm) },
                            onToggle = { viewModel.updateAlarmEnabled(alarm, !alarm.isEnabled) },
                            onEdit = { navController.navigate("edit_alarm/${alarm.id}") }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AlarmCard(
    alarm: Alarm,
    onDelete: () -> Unit,
    onToggle: () -> Unit,
    onEdit: () -> Unit
) {
    val nextDateTime = remember(alarm, ShiftCalculator.startDateString) {
        ShiftCalculator.getNextAlarmDateTime(
            alarm.hour,
            alarm.minute,
            alarm.shiftType,
            alarm.shiftCycle
        )
    }
    val nextDateText = if (nextDateTime != null) {
        ShiftCalculator.formatAlarmDateTime(nextDateTime)
    } else {
        "Не определено"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = alarm.label,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1
                )
                Text(
                    text = String.format("%02d:%02d", alarm.hour, alarm.minute),
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "Следующий: $nextDateText",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = when (alarm.shiftType) {
                        ShiftType.WORK_DAY -> "Режим: только рабочие дни"
                        ShiftType.DAY_OFF -> "Режим: только выходные дни"
                        ShiftType.ALL -> "Режим: каждый день"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = alarm.isEnabled,
                    onCheckedChange = { onToggle() }
                )
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Редактировать")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Удалить")
                }
            }
        }
    }
}