package com.example.shiftalarm.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.shiftalarm.data.Alarm
import com.example.shiftalarm.viewmodel.AlarmViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmListScreen(
    navController: NavController,
    viewModel: AlarmViewModel
) {
    val alarms by viewModel.alarms.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("edit_alarm/-1") }) {
                Icon(Icons.Default.Add, contentDescription = "Добавить")
            }
        },
        topBar = {
            TopAppBar(
                title = { Text("Мои будильники") },
                actions = {
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(Icons.Default.Settings, contentDescription = "Настройки")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(alarms) { alarm ->
                AlarmCard(
                    alarm = alarm,
                    onToggle = { enabled -> viewModel.updateAlarmEnabled(alarm, enabled) },
                    onClick = { navController.navigate("edit_alarm/${alarm.id}") },
                    onDelete = { viewModel.deleteAlarm(alarm) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmCard(
    alarm: Alarm,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = String.format("%02d:%02d", alarm.hour, alarm.minute),
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(text = alarm.label, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = when (alarm.shiftCycle) {
                        2 -> "График 2/2: ${alarm.shiftType.name}"
                        3 -> "График 3/3: ${alarm.shiftType.name}"
                        else -> "Обычный"
                    },
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Row {
                Switch(checked = alarm.isEnabled, onCheckedChange = onToggle)
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Удалить")
                }
            }
        }
    }
}