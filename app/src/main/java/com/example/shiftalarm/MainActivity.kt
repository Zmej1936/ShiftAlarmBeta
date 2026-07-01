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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.shiftalarm.data.AlarmRepository
import com.example.shiftalarm.receivers.AlarmStopReceiver
import com.example.shiftalarm.screens.AboutScreen
import com.example.shiftalarm.screens.AlarmListScreen
import com.example.shiftalarm.screens.EditAlarmScreen
import com.example.shiftalarm.screens.SettingsScreen
import com.example.shiftalarm.ui.theme.ShiftAlarmTheme
import com.example.shiftalarm.utils.AlarmScheduler
import com.example.shiftalarm.utils.ShiftCalculator
import com.example.shiftalarm.viewmodel.AlarmViewModel
import com.example.shiftalarm.viewmodel.AlarmViewModelFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.datastore.preferences.core.stringPreferencesKey

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    private val exactAlarmLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ShiftAlarmTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppNavigation()
                }
            }
        }

        if (intent?.getBooleanExtra("stop_alarm", false) == true) {
            AlarmStopReceiver.mediaPlayer?.stop()
            AlarmStopReceiver.mediaPlayer?.release()
            AlarmStopReceiver.mediaPlayer = null
            (getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).cancel()
        }

        requestPermissions()
        lifecycleScope.launch {
            val prefs = applicationContext.settingsDataStore.data.first()
            val startDate = prefs[stringPreferencesKey("start_date")] ?: "2026-05-20"
            val repository = AlarmRepository(applicationContext)
            val viewModel = AlarmViewModel(repository)
            val alarms = viewModel.alarms.first()
            val scheduler = AlarmScheduler(this@MainActivity)
            alarms.filter { it.isEnabled }.forEach { alarm ->
                scheduler.scheduleAlarm(alarm)
            }
        }
    }

    @Composable
    fun AppNavigation() {
        val navController = rememberNavController()
        val repository = AlarmRepository(applicationContext)
        val factory = AlarmViewModelFactory(repository)
        // Правильное создание ViewModel через фабрику
        val viewModel: AlarmViewModel = viewModel(factory = factory)

        NavHost(navController, startDestination = "alarm_list") {
            composable("alarm_list") { AlarmListScreen(navController, viewModel) }
            composable("edit_alarm/{alarmId}") { backStackEntry ->
                val alarmId = backStackEntry.arguments?.getString("alarmId")?.toIntOrNull() ?: -1
                EditAlarmScreen(navController, viewModel, alarmId)
            }
            composable("settings") { SettingsScreen(navController) }
            composable("about") { AboutScreen(onBack = { navController.popBackStack() }) }
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