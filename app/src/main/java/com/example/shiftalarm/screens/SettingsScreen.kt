package com.example.shiftalarm.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.shiftalarm.settingsDataStore
import com.example.shiftalarm.utils.ShiftCalculator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var startDate by remember { mutableStateOf("2026-05-20") }

    LaunchedEffect(Unit) {
        val prefs = context.settingsDataStore.data.first()
        startDate = prefs[stringPreferencesKey("start_date")] ?: "2026-05-20"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Дата начала графика (ГГГГ-ММ-ДД):", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = startDate,
                onValueChange = { startDate = it },
                label = { Text("Например, 2026-05-20") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    scope.launch {
                        context.settingsDataStore.edit { prefs ->
                            prefs[stringPreferencesKey("start_date")] = startDate
                        }
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Сохранить дату")
            }
        }
    }
}