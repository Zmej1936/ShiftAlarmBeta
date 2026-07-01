package com.example.shiftalarm.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.shiftalarm.data.Alarm
import com.example.shiftalarm.data.AlarmRepository
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
    val context = LocalContext.current
    val alarmState = viewModel.getAlarmById(alarmId).collectAsStateWithLifecycle(initialValue = null)
    val alarm = alarmState.value

    var label by remember { mutableStateOf("") }
    var hour by remember { mutableStateOf(8) }
    var minute by remember { mutableStateOf(0) }
    var shiftType by remember { mutableStateOf(ShiftType.WORK_DAY) }
    var shiftCycle by remember { mutableStateOf(2) }

    var selectedDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }

    var showTimePicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var dropdownExpanded by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val formatter = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }

    LaunchedEffect(alarm) {
        alarm?.let {
            label = it.label
            hour = it.hour
            minute = it.minute
            shiftType = it.shiftType
            shiftCycle = it.shiftCycle
            try {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(it.startDate ?: "2026-05-20")?.time?.let { millis ->
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
                title = { Text("Параметры будильника") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        },
        floatingActionButton = {
            Button(
                onClick = {
                    scope.launch {
                        val dbDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val formattedDate = dbDateFormat.format(Date(selectedDateMillis))

                        val alarmToSave = Alarm(
                            id = if (alarmId == -1) System.currentTimeMillis().toInt() else alarmId,
                            hour = hour,
                            minute = minute,
                            label = label,
                            shiftType = shiftType,
                            shiftCycle = shiftCycle,
                            isEnabled = true,
                            startDate = formattedDate
                        )

                        val scheduler = AlarmScheduler(context)
                        scheduler.scheduleAlarm(alarmToSave)

                        val currentAlarms = AlarmRepository.alarmsFlow.value.toMutableList()
                        if (alarmId == -1) {
                            currentAlarms.add(alarmToSave)
                        } else {
                            val idx = currentAlarms.indexOfFirst { it.id == alarmId }
                            if (idx != -1) currentAlarms[idx] = alarmToSave else currentAlarms.add(alarmToSave)
                        }

                        AlarmRepository.saveAlarms(context, currentAlarms)
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.padding(16.dp)
            ) {
                Text("Сохранить", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("Название будильника") },
                modifier = Modifier.fillMaxWidth()
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showTimePicker = true },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Время срабатывания", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = String.format(Locale.US, "%02d:%02d", hour, minute),
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Дата начала графика смен", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatter.format(Date(selectedDateMillis)),
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            OutlinedTextField(
                value = if (shiftCycle == 0) "" else shiftCycle.toString(),
                onValueChange = { shiftCycle = it.toIntOrNull() ?: 0 },
                label = { Text("Периодичность цикла (дней)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Box(modifier = Modifier.fillMaxWidth()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { dropdownExpanded = true },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Тип смены", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = when(shiftType) {
                                ShiftType.WORK_DAY -> "Рабочий день"
                                ShiftType.OFF_DAY -> "Выходной день"
                                ShiftType.ALL -> "Каждый день"
                            },
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                DropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    DropdownMenuItem(
                        text = { Text("Рабочий день") },
                        onClick = { shiftType = ShiftType.WORK_DAY; dropdownExpanded = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Выходной день") },
                        onClick = { shiftType = ShiftType.OFF_DAY; dropdownExpanded = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Каждый день") },
                        onClick = { shiftType = ShiftType.ALL; dropdownExpanded = false }
                    )
                }
            }
        }
    }

    // Диалог выбора времени
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = hour,
            initialMinute = minute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        hour = timePickerState.hour
                        minute = timePickerState.minute
                        showTimePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Отмена")
                }
            },
            text = { TimePicker(state = timePickerState) }
        )
    }

    // Диалог выбора даты
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDateMillis
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { selectedDateMillis = it }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Отмена")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}