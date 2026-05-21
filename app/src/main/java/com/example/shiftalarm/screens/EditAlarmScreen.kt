package com.example.shiftalarm.ui.screens

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.shiftalarm.data.Alarm
import com.example.shiftalarm.data.ShiftType
import com.example.shiftalarm.viewmodel.AlarmViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAlarmScreen(
    navController: NavController,
    viewModel: AlarmViewModel,
    alarmId: Int
) {
    val context = LocalContext.current
    val selectedAlarm by viewModel.selectedAlarm.collectAsState()
    val scope = rememberCoroutineScope()

    var hour by remember { mutableStateOf(7) }
    var minute by remember { mutableStateOf(0) }
    var label by remember { mutableStateOf("") }
    var shiftType by remember { mutableStateOf(ShiftType.WORK_DAY_1) }
    var shiftCycle by remember { mutableStateOf(2) }
    var isEnabled by remember { mutableStateOf(true) }
    var showTimePicker by remember { mutableStateOf(false) }

    LaunchedEffect(alarmId) {
        if (alarmId != -1) viewModel.getAlarmById(alarmId)
    }

    LaunchedEffect(selectedAlarm) {
        selectedAlarm?.let {
            hour = it.hour
            minute = it.minute
            label = it.label
            shiftType = it.shiftType
            shiftCycle = it.shiftCycle
            isEnabled = it.isEnabled
        }
    }

    fun saveAlarm() {
        val alarm = Alarm(
            id = if (alarmId != -1) alarmId else System.currentTimeMillis().toInt(),
            hour = hour,
            minute = minute,
            label = label.ifBlank { "Будильник" },
            shiftType = shiftType,
            shiftCycle = shiftCycle,
            isEnabled = isEnabled
        )
        scope.launch {
            viewModel.insertAlarm(alarm)
            viewModel.clearSelectedAlarm()
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (alarmId == -1) "Новый будильник" else "Редактировать") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Text("Назад")
                    }
                },
                actions = {
                    TextButton(onClick = { saveAlarm() }) {
                        Text("Сохранить")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Время
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Время", style = MaterialTheme.typography.titleMedium)
                    Button(
                        onClick = { showTimePicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Выбрать время: ${String.format("%02d:%02d", hour, minute)}")
                    }
                    if (showTimePicker) {
                        TimePickerDialog(
                            context,
                            { _, selectedHour, selectedMinute ->
                                hour = selectedHour
                                minute = selectedMinute
                                showTimePicker = false
                            },
                            hour,
                            minute,
                            true
                        ).apply {
                            setOnDismissListener { showTimePicker = false }
                            show()
                        }
                    }
                }
            }

            // График
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("График работы", style = MaterialTheme.typography.titleMedium)
                    Row {
                        FilterChip(
                            selected = shiftCycle == 2,
                            onClick = { shiftCycle = 2 },
                            label = { Text("2/2") }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FilterChip(
                            selected = shiftCycle == 3,
                            onClick = { shiftCycle = 3 },
                            label = { Text("3/3") }
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Будильник для:", style = MaterialTheme.typography.bodyMedium)
                    ShiftTypeRadioGroup(
                        selectedType = shiftType,
                        cycle = shiftCycle,
                        onTypeSelected = { shiftType = it }
                    )
                }
            }

            // Название
            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("Название (необязательно)") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun ShiftTypeRadioGroup(
    selectedType: ShiftType,
    cycle: Int,
    onTypeSelected: (ShiftType) -> Unit
) {
    val types = when (cycle) {
        2 -> listOf(ShiftType.WORK_DAY_1, ShiftType.WORK_DAY_2, ShiftType.DAY_OFF, ShiftType.ALL)
        3 -> listOf(ShiftType.WORK_DAY_1, ShiftType.WORK_DAY_2, ShiftType.WORK_DAY_3, ShiftType.DAY_OFF, ShiftType.ALL)
        else -> ShiftType.entries.toList()
    }
    Column {
        types.forEach { type ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = selectedType == type,
                    onClick = { onTypeSelected(type) }
                )
                Text(
                    text = when (type) {
                        ShiftType.WORK_DAY_1 -> "Рабочий день 1"
                        ShiftType.WORK_DAY_2 -> "Рабочий день 2"
                        ShiftType.WORK_DAY_3 -> "Рабочий день 3"
                        ShiftType.DAY_OFF -> "Выходной"
                        ShiftType.ALL -> "Каждый день"
                    },
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}