package com.example.shiftalarm

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.shiftalarm.data.AlarmRepository
import com.example.shiftalarm.receivers.AlarmSoundService
import com.example.shiftalarm.receivers.AlarmStopReceiver
import com.example.shiftalarm.receivers.SnoozeReceiver
import com.example.shiftalarm.screens.AlarmListScreen
import com.example.shiftalarm.screens.EditAlarmScreen
import com.example.shiftalarm.utils.AlarmScheduler
import com.example.shiftalarm.viewmodel.AlarmViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    private var isAlarmActive by mutableStateOf(false)
    private var activeAlarmId by mutableIntStateOf(-1)
    private var activeAlarmLabel by mutableStateOf("Будильник смен")
    private var activeAlarmTime by mutableStateOf("08:00")

    override fun onCreate(savedInstanceState: Bundle?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        super.onCreate(savedInstanceState)

        AlarmRepository.initialize(this)
        AlarmScheduler(this).rescheduleAllAlarms()

        checkAlarmIntent(intent)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (!notificationManager.canUseFullScreenIntent()) {
                val intent = Intent(android.provider.Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                    data = Uri.parse("package:$packageName")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
            }
        }

        if (!android.provider.Settings.canDrawOverlays(this)) {
            val overlayIntent = Intent(
                android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(overlayIntent)
        }

        val fixedDarkColorScheme = darkColorScheme(
            primary = Color(0xFFD0BCFF),
            onPrimary = Color(0xFF381E72),
            primaryContainer = Color(0xFF4F378B),
            onPrimaryContainer = Color(0xFFEADDFF),
            surface = Color(0xFF1C1B1F),
            onSurface = Color(0xFFE6E1E5),
            surfaceVariant = Color(0xFF49454F),
            onSurfaceVariant = Color(0xFFCAC4D0),
            background = Color(0xFF141218),
            onBackground = Color(0xFFE6E1E5),
            outline = Color(0xFF938F99)
        )

        setContent {
            MaterialTheme(colorScheme = fixedDarkColorScheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (isAlarmActive) {
                        FullScreenAlarmScreen(
                            label = activeAlarmLabel,
                            onStopClick = {
                                val serviceIntent = Intent(this@MainActivity, AlarmSoundService::class.java)
                                stopService(serviceIntent)
                                val stopIntent = Intent(this@MainActivity, AlarmStopReceiver::class.java).apply {
                                    putExtra("alarm_id", activeAlarmId)
                                }
                                sendBroadcast(stopIntent)
                                isAlarmActive = false
                            },
                            onSnoozeClick = {
                                val serviceIntent = Intent(this@MainActivity, AlarmSoundService::class.java)
                                stopService(serviceIntent)
                                val snoozeIntent = Intent(this@MainActivity, SnoozeReceiver::class.java).apply {
                                    putExtra("alarm_id", activeAlarmId)
                                    putExtra("label", activeAlarmLabel)
                                }
                                sendBroadcast(snoozeIntent)
                                isAlarmActive = false
                            }
                        )
                    } else {
                        val navController = rememberNavController()
                        val alarmViewModel: AlarmViewModel = viewModel()

                        NavHost(navController = navController, startDestination = "alarm_list") {
                            composable("alarm_list") {
                                AlarmListScreen(navController = navController, viewModel = alarmViewModel)
                            }
                            composable("edit_alarm/{alarmId}") { backStackEntry ->
                                val alarmId = backStackEntry.arguments?.getString("alarmId")?.toIntOrNull() ?: -1
                                EditAlarmScreen(navController = navController, viewModel = alarmViewModel, alarmId = alarmId)
                            }

                            composable("about") {
                                val scope = rememberCoroutineScope()
                                val currentContext = LocalContext.current

                                var isUpdateAvailable by remember { mutableStateOf(false) }
                                var serverVersionName by remember { mutableStateOf("") }

                                LaunchedEffect(Unit) {
                                    scope.launch(Dispatchers.IO) {
                                        try {
                                            // ✅ ПРАВИЛЬНЫЙ URL для API GitHub
                                            val url = URL("https://api.github.com/repos/Zmej1936/ShiftAlarmBeta/contents/version.json")
                                            val connection = url.openConnection() as HttpURLConnection
                                            connection.requestMethod = "GET"
                                            connection.setRequestProperty("User-Agent", "ShiftAlarm-App")
                                            // ✅ Добавляем заголовок Accept для корректного ответа от API
                                            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
                                            connection.connectTimeout = 5000
                                            connection.readTimeout = 5000

                                            val jsonText = connection.inputStream.bufferedReader().use { it.readText() }
                                            val responseJson = JSONObject(jsonText)

                                            val base64Content = responseJson.getString("content").replace("\n", "").replace(" ", "")
                                            val decodedBytes = android.util.Base64.decode(base64Content, android.util.Base64.DEFAULT)
                                            val rawJsonText = String(decodedBytes, Charsets.UTF_8)

                                            val json = JSONObject(rawJsonText)
                                            val serverVersionCode = json.getInt("versionCode")
                                            serverVersionName = json.getString("versionName")

                                            val currentVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                                currentContext.packageManager.getPackageInfo(currentContext.packageName, 0).longVersionCode.toInt()
                                            } else {
                                                @Suppress("DEPRECATION")
                                                currentContext.packageManager.getPackageInfo(currentContext.packageName, 0).versionCode
                                            }

                                            if (serverVersionCode > currentVersionCode) {
                                                withContext(Dispatchers.Main) {
                                                    isUpdateAvailable = true
                                                }
                                            }
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                }

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
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.padding(16.dp)
                                        ) {
                                            Text(text = "Shift Alarm", style = MaterialTheme.typography.headlineMedium)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "Умный будильник для сменных графиков",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Text(
                                                text = "Версия приложения: ${BuildConfig.VERSION_NAME}",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.height(48.dp))

                                            if (isUpdateAvailable) {
                                                Button(
                                                    onClick = {
                                                        val updateIntent = Intent(Intent.ACTION_VIEW).apply {
                                                            // ✅ ПРЯМАЯ ССЫЛКА НА APK ИЗ ПОСЛЕДНЕГО РЕЛИЗА
                                                            data = Uri.parse("https://github.com/Zmej1936/ShiftAlarmBeta/releases/latest/download/app-debug.apk")
                                                        }
                                                        currentContext.startActivity(updateIntent)
                                                    },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                                    ),
                                                    modifier = Modifier
                                                        .fillMaxWidth(0.7f)
                                                        .height(50.dp)
                                                ) {
                                                    Text(
                                                        "ОБНОВИТЬ ДО $serverVersionName 🔄",
                                                        fontSize = 16.sp,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(16.dp))
                                            }

                                            Button(
                                                onClick = {
                                                    val donateIntent = Intent(Intent.ACTION_VIEW).apply {
                                                        // ✅ ПРАВИЛЬНАЯ ССЫЛКА НА DONATIONALERTS
                                                        data = Uri.parse("https://www.donationalerts.com/r/zmej1936")
                                                    }
                                                    currentContext.startActivity(donateIntent)
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00A86B)),
                                                modifier = Modifier
                                                    .fillMaxWidth(0.7f)
                                                    .height(50.dp)
                                            ) {
                                                Text(
                                                    "ПОДДЕРЖАТЬ АВТОРА ☕",
                                                    fontSize = 16.sp,
                                                    color = Color.White
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
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        checkAlarmIntent(intent)
    }

    private fun checkAlarmIntent(intent: Intent?) {
        if (intent != null && intent.getBooleanExtra("is_alarm_trigger", false)) {
            activeAlarmId = intent.getIntExtra("alarm_id", -1)
            activeAlarmLabel = intent.getStringExtra("label") ?: "Будильник смен"
            val h = intent.getIntExtra("hour", 8)
            val m = intent.getIntExtra("minute", 0)
            activeAlarmTime = String.format(Locale.US, "%02d:%02d", h, m)
            isAlarmActive = true
        }
    }
}

@Composable
fun FullScreenAlarmScreen(
    label: String,
    onStopClick: () -> Unit,
    onSnoozeClick: () -> Unit
) {
    var currentTimeText by remember {
        mutableStateOf(java.text.SimpleDateFormat("HH:mm", Locale.US).format(java.util.Date()))
    }

    LaunchedEffect(Unit) {
        while (true) {
            currentTimeText = java.text.SimpleDateFormat("HH:mm", Locale.US).format(java.util.Date())
            kotlinx.coroutines.delay(1000)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = currentTimeText,
                fontSize = 72.sp,
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = label,
                fontSize = 24.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(64.dp))

            Button(
                onClick = onStopClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBA1A1A)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            ) {
                Text("ОТКЛЮЧИТЬ", fontSize = 18.sp, color = Color.White)
            }
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onSnoozeClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Text("ОТЛОЖИТЬ 10 МИН", fontSize = 16.sp, color = Color.White)
            }
        }
    }
}