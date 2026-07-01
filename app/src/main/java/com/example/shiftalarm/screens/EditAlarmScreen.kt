package com.example.shiftalarm.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.shiftalarm.data.Alarm
import com.example.shiftalarm.data.ShiftType
import com.example.shiftalarm.utils.AlarmScheduler
import com.example.shiftalarm.viewmodel.AlarmViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAlarmScreen(
    navController: NavController,
    viewModel: AlarmViewModel,
    alarmId: Int
) {
    val alarmState = viewModel.getAlarmById(alarmId).collectAsStateWithLifecycle(initialValue = null)
    val alarm = alarmState.value

    var label by remember { mutableStateOf("") }
    var hour by remember { mutableStateOf(8) }
    var minute by remember { mutableStateOf(0) }
    var shiftType by remember { mutableStateOf(ShiftType.WORK_DAY) }
    var shiftCycle by remember { mutableStateOf(2) }

    // Состояние индивидуальной даты старта
    var selectedDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }

    var showTimePicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val formatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    LaunchedEffect(alarm) {
        alarm?.let {
            label = it.label
            hour = it.hour
            minute = it.minute
            shiftType = it.shiftType
            shiftCycle = it.shiftCycle

            // ИСПРАВЛЕНО: Безопасное извлечение с элвис-оператором
            val dateToParse = it.startDate ?: "2026-05-20"
            try {
                formatter.parse(dateToParse)?.time?.let { millis ->
                    selectedDateMillis = millis
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (alarmId == -1) "Новый будильник" else "Редактировать") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                scope.launch {
                    val formattedDate = formatter.format(Date(selectedDateMillis))

                    val alarmToSave = if (alarmId == -1) {
                        Alarm(
                            id = System.currentTimeMillis().toInt(),
                            hour = hour,
                            minute = minute,
                            label = label,
                            shiftType = shiftType,
                            shiftCycle = shiftCycle,
                            isEnabled = true,
                            startDate = formattedDate
                        )
                    } else {
                        // ИСПРАВЛЕНО: Безопасное копирование
                        alarm?.copy(
                            hour = hour,
                            minute = minute,
                            label = label,
                            shiftType = shiftType,
                            shiftCycle = shiftCycle,
                            startDate = formattedDate
                        ) ?: Alarm(
                            id = alarmId,
                            hour = hour,
                            minute = minute,
                            label = label,
                            shiftType = shiftType,
                            shiftCycle = shiftCycle,
                            isEnabled = true,
                            startDate = formattedDate
                        )
                    }

                    viewModel.insertAlarm(alarmToSave)
                    AlarmScheduler(navController.context).scheduleAlarm(alarmToSave)
                    navController.popBackStack()
                }
            }) {
                Text(modifier = Modifier.padding(horizontal = 16.dp), text = "Сохранить")
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
                label = { Text("Название будильника") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = String.format("%02d:%02d", hour, minute),
                onValueChange = {},
                label = { Text("Время срабатывания") },
                readOnly = true,
                enabled = false,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showTimePicker = true }
            )

            // Поле выбора индивидуальной даты начала графика
            OutlinedTextField(
                value = formatter.format(Date(selectedDateMillis)),
                onValueChange = {},
                label = { Text("Дата начала графика смен") },
                readOnly = true,
                enabled = false,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true }
            )

            OutlinedTextField(
                value = shiftCycle.toString(),
                onValueChange = { shiftCycle = it.toIntOrNull() ?: 2 },
                label = { Text("Периодичность цикла (дней)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Text(text = "Текущий тип смены: ${shiftType.name}", style = MaterialTheme.typography.bodyMedium)
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    hour = timePickerState.hour
                    minute = timePickerState.minute
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Отмена") } },
            text = { TimePicker(state = timePickerState) }
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { selectedDateMillis = it }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Отмена") } }
        ) { DatePicker(state = datePickerState) }
    }
}