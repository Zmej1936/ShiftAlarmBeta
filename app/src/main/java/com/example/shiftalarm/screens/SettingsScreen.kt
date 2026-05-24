package com.example.shiftalarm.ui.screens

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.navigation.NavController
import com.example.shiftalarm.settingsDataStore
import com.example.shiftalarm.utils.ShiftCalculator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var startDate by remember { mutableStateOf("2026-05-20") }
    var shiftCycle by remember { mutableStateOf(2) }  // 2 или 3
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val prefs = context.settingsDataStore.data.first()
        startDate = prefs[stringPreferencesKey("start_date")] ?: "2026-05-20"
        shiftCycle = prefs[intPreferencesKey("shift_cycle")] ?: 2
        ShiftCalculator.setStartDate(startDate)
    }

    val parts = startDate.split("-")
    val currentYear = parts.getOrNull(0)?.toIntOrNull() ?: 2026
    val currentMonth = parts.getOrNull(1)?.toIntOrNull() ?: 5
    val currentDay = parts.getOrNull(2)?.toIntOrNull() ?: 19

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки графика") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Text("Назад")
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
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Дата первого рабочего дня", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { showDatePicker = true }) {
                        Text("Выбрать дату: $startDate")
                    }
                    if (showDatePicker) {
                        DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                val formattedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                                scope.launch {
                                    context.settingsDataStore.edit { prefs ->
                                        prefs[stringPreferencesKey("start_date")] = formattedDate
                                    }
                                    startDate = formattedDate
                                    ShiftCalculator.setStartDate(formattedDate)
                                }
                                showDatePicker = false
                            },
                            currentYear, currentMonth - 1, currentDay
                        ).apply {
                            setOnDismissListener { showDatePicker = false }
                            show()
                        }
                    }
                }
            }

            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("График работы", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = shiftCycle == 2,
                            onClick = {
                                shiftCycle = 2
                                scope.launch {
                                    context.settingsDataStore.edit { prefs ->
                                        prefs[intPreferencesKey("shift_cycle")] = 2
                                    }
                                }
                            },
                            label = { Text("2 рабочих / 2 выходных") }
                        )
                        FilterChip(
                            selected = shiftCycle == 3,
                            onClick = {
                                shiftCycle = 3
                                scope.launch {
                                    context.settingsDataStore.edit { prefs ->
                                        prefs[intPreferencesKey("shift_cycle")] = 3
                                    }
                                }
                            },
                            label = { Text("3 рабочих / 3 выходных") }
                        )
                    }
                }
            }

            Button(
                onClick = {
                    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Отключить оптимизацию батареи (рекомендуется)")
            }
        }
    }
}