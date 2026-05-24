package com.example.shiftalarm

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Vibrator
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.shiftalarm.data.AlarmRepository
import com.example.shiftalarm.receivers.AlarmStopReceiver
import com.example.shiftalarm.ui.screens.AlarmListScreen
import com.example.shiftalarm.ui.screens.EditAlarmScreen
import com.example.shiftalarm.ui.screens.SettingsScreen
import com.example.shiftalarm.ui.theme.ShiftAlarmTheme
import com.example.shiftalarm.utils.AlarmScheduler
import com.example.shiftalarm.utils.ShiftCalculator
import com.example.shiftalarm.viewmodel.AlarmViewModel
import com.example.shiftalarm.viewmodel.AlarmViewModelFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey


class MainActivity : ComponentActivity() {

    private val repository by lazy { AlarmRepository(applicationContext) }
    private val factory by lazy { AlarmViewModelFactory(repository) }
    private val viewModel: AlarmViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return factory.create() as T
            }
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    private val exactAlarmLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (alarmManager.canScheduleExactAlarms()) { }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ShiftAlarmTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(viewModel = viewModel)
                }
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }

        if (intent?.getBooleanExtra("stop_alarm", false) == true) {
            AlarmStopReceiver.mediaPlayer?.stop()
            AlarmStopReceiver.mediaPlayer?.release()
            AlarmStopReceiver.mediaPlayer = null
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibrator.cancel()
        }

        requestPermissions()
        lifecycleScope.launch {
            try {
                val prefs = applicationContext.settingsDataStore.data.first()
                val startDate = prefs[stringPreferencesKey("start_date")] ?: "2026-05-20"
                ShiftCalculator.setStartDate(startDate)

                val globalShiftCycle = prefs[intPreferencesKey("shift_cycle")] ?: 2

                // Перепланируем все активные будильники после запуска
                val alarms = viewModel.alarms.first()
                alarms.filter { it.isEnabled }.forEach { alarm ->
                    AlarmScheduler(this@MainActivity).scheduleAlarm(alarm, globalShiftCycle)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @Composable
    fun AppNavigation(viewModel: AlarmViewModel) {
        val navController = rememberNavController()
        NavHost(navController = navController, startDestination = "alarm_list") {
            composable("alarm_list") {
                AlarmListScreen(navController, viewModel)
            }
            composable("edit_alarm/{alarmId}") { backStackEntry ->
                val alarmId = backStackEntry.arguments?.getString("alarmId")?.toIntOrNull() ?: -1
                EditAlarmScreen(navController, viewModel, alarmId)
            }
            composable("settings") {
                SettingsScreen(navController)
            }
        }
    }

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                exactAlarmLauncher.launch(intent)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.USE_FULL_SCREEN_INTENT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(Manifest.permission.USE_FULL_SCREEN_INTENT), 100)
            }
        }
    }
}