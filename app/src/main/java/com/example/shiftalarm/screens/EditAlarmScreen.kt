package com.example.shiftalarm.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import androidx.navigation.NavController
import com.example.shiftalarm.data.Alarm
import com.example.shiftalarm.data.ShiftType
import com.example.shiftalarm.settingsDataStore
import com.example.shiftalarm.utils.AlarmScheduler
import com.example.shiftalarm.viewmodel.AlarmViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.datastore.preferences.core.intPreferencesKey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAlarmScreen(
    navController: NavController,
    viewModel: AlarmViewModel,
    alarmId: Int
) {
    val context = LocalContext.current
    val alarm by viewModel.getAlarmById(alarmId).collectAsState()

    var label by remember(alarm) { mutableStateOf(alarm?.label ?: "") }
    var hourText by remember(alarm) { mutableStateOf((alarm?.hour ?: 8).toString()) }
    var minuteText by remember(alarm) { mutableStateOf((alarm?.minute ?: 0).toString().padStart(2, '0')) }
    var shiftType by remember(alarm) { mutableStateOf(alarm?.shiftType ?: ShiftType.WORK_DAY) }

    val scope = rememberCoroutineScope()

    fun toIntOrZero(value: String): Int = value.toIntOrNull() ?: 0
    fun validateHour(hour: Int): Int = hour.coerceIn(0, 23)
    fun validateMinute(minute: Int): Int = minute.coerceIn(0, 59)

    Scaffold(
        topBar = { TopAppBar(title = { Text(if (alarmId == -1) "Новый будильник" else "Редактировать будильник") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                scope.launch {
                    val hour = validateHour(toIntOrZero(hourText))
                    val minute = validateMinute(toIntOrZero(minuteText))

                    // Получаем глобальный сдвиг цикла из настроек
                    val prefs = context.settingsDataStore.data.first()
                    val globalShiftCycle = prefs[intPreferencesKey("shift_cycle")] ?: 2

                    val newAlarm = if (alarmId == -1) {
                        Alarm(
                            id = (System.currentTimeMillis() and 0x7FFFFFFF).toInt(),
                            hour = hour,
                            minute = minute,
                            label = label,
                            shiftType = shiftType,
                            shiftCycle = globalShiftCycle,
                            isEnabled = true
                        )
                    } else {
                        alarm?.copy(
                            hour = hour,
                            minute = minute,
                            label = label,
                            shiftType = shiftType,
                            shiftCycle = globalShiftCycle
                        ) ?: return@launch
                    }
                    viewModel.insertAlarm(newAlarm)

                    if (newAlarm.isEnabled) {
                        val scheduler = AlarmScheduler(context)
                        scheduler.scheduleAlarm(newAlarm, globalShiftCycle)
                    }
                    navController.popBackStack()
                }
            }) {
                Text("Сохранить")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("Название") },
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = hourText,
                    onValueChange = { newText ->
                        if (newText.isEmpty() || newText.all { it.isDigit() }) {
                            hourText = newText.take(2)
                        }
                    },
                    label = { Text("Час (0-23)") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = toIntOrZero(hourText) !in 0..23 && hourText.isNotEmpty()
                )
                OutlinedTextField(
                    value = minuteText,
                    onValueChange = { newText ->
                        if (newText.isEmpty() || newText.all { it.isDigit() }) {
                            minuteText = newText.take(2)
                        }
                    },
                    label = { Text("Минута (0-59)") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = toIntOrZero(minuteText) !in 0..59 && minuteText.isNotEmpty()
                )
            }
            Text("Тип будильника:", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = shiftType == ShiftType.WORK_DAY,
                    onClick = { shiftType = ShiftType.WORK_DAY },
                    label = { Text("Только рабочие дни") }
                )
                FilterChip(
                    selected = shiftType == ShiftType.DAY_OFF,
                    onClick = { shiftType = ShiftType.DAY_OFF },
                    label = { Text("Только выходные дни") }
                )
                FilterChip(
                    selected = shiftType == ShiftType.ALL,
                    onClick = { shiftType = ShiftType.ALL },
                    label = { Text("Каждый день") }
                )
            }
        }
    }
}