package com.example.shiftalarm.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.shiftalarm.data.Alarm
import com.example.shiftalarm.settingsDataStore
import com.example.shiftalarm.utils.ShiftCalculator
import com.example.shiftalarm.viewmodel.AlarmViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.datastore.preferences.core.stringPreferencesKey

@Composable
fun AlarmListScreen(
    navController: NavController,
    viewModel: AlarmViewModel
) {
    val alarms by viewModel.alarms.collectAsStateWithLifecycle(initialValue = emptyList())
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var startDate by remember { mutableStateOf("2026-05-20") }

    LaunchedEffect(Unit) {
        val prefs = context.settingsDataStore.data.first()
        startDate = prefs[stringPreferencesKey("start_date")] ?: "2026-05-20"
        ShiftCalculator.setStartDate(startDate)
    }

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
                    text = "Тип: ${alarm.shiftType} / Цикл: ${alarm.shiftCycle}",
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