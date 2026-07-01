package com.example.shiftalarm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.shiftalarm.data.AlarmRepository
import com.example.shiftalarm.screens.AlarmListScreen
import com.example.shiftalarm.screens.EditAlarmScreen
import com.example.shiftalarm.utils.AlarmScheduler
import com.example.shiftalarm.viewmodel.AlarmViewModel

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AlarmRepository.initialize(this)
        AlarmScheduler(this).rescheduleAllAlarms()

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val alarmViewModel: AlarmViewModel = viewModel()

                    NavHost(navController = navController, startDestination = "alarm_list") {
                        composable("alarm_list") {
                            AlarmListScreen(navController = navController, viewModel = alarmViewModel)
                        }

                        composable("edit_alarm/{alarmId}") { backStackEntry ->
                            val alarmId = backStackEntry.arguments?.getString("alarmId")?.toIntOrNull() ?: -1
                            EditAlarmScreen(
                                navController = navController,
                                viewModel = alarmViewModel,
                                alarmId = alarmId
                            )
                        }

                        // ИСПРАВЛЕНО: Аккуратный Material 3 экран "О программе" вместо текстовой заглушки
                        composable("about") {
                            Scaffold(
                                topBar = {
                                    TopAppBar(
                                        title = { Text("О программе") },
                                        navigationIcon = {
                                            IconButton(onClick = { navController.popBackStack() }) {
                                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                                            }
                                        }
                                    )
                                }
                            ) { padding ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(padding),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "Shift Alarm",
                                            style = MaterialTheme.typography.headlineMedium
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Умный будильник для сменных графиков",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = "Версия: ${BuildConfig.VERSION_NAME}",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}