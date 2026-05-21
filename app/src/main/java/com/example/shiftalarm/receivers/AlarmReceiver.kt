package com.example.shiftalarm.receivers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.shiftalarm.MainActivity
import com.example.shiftalarm.R
import com.example.shiftalarm.data.Alarm
import com.example.shiftalarm.data.ShiftType
import com.example.shiftalarm.utils.AlarmScheduler
import com.example.shiftalarm.utils.ShiftCalculator

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        var wakeLock: PowerManager.WakeLock? = null
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("AlarmDebug", "=== AlarmReceiver triggered ===")

        // Включаем экран и удерживаем его
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "AlarmWakeLock"
        ).apply {
            acquire(60_000L) // удерживаем 60 секунд
        }

        val alarmId = intent.getIntExtra("alarm_id", 0)
        val hour = intent.getIntExtra("alarm_hour", 0)
        val minute = intent.getIntExtra("alarm_minute", 0)
        val label = intent.getStringExtra("alarm_label") ?: "Будильник"
        val shiftTypeName = intent.getStringExtra("alarm_shift_type") ?: "ALL"
        val shiftCycle = intent.getIntExtra("alarm_shift_cycle", 2)

        val shiftType = try {
            ShiftType.valueOf(shiftTypeName)
        } catch (e: Exception) {
            ShiftType.ALL
        }

        // Проверка, должен ли сегодня сработать будильник по графику
        val shouldRing = ShiftCalculator.shouldAlarmRingToday(shiftType, shiftCycle)
        Log.d("AlarmDebug", "shouldRing = $shouldRing, shiftType=$shiftType, shiftCycle=$shiftCycle")

        if (!shouldRing) {
            Log.d("AlarmDebug", "Сегодня не подходящий день, перепланируем")
            val tempAlarm = Alarm(
                id = alarmId, hour = hour, minute = minute, label = label,
                shiftType = shiftType, shiftCycle = shiftCycle, isEnabled = true
            )
            val scheduler = AlarmScheduler(context)
            scheduler.cancelAlarm(tempAlarm)
            scheduler.scheduleAlarm(tempAlarm)
            wakeLock?.release()
            wakeLock = null
            return
        }

        // Вибрация (длинная, повторяющаяся)
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 1000, 500, 1000, 500, 2000), 0))
        } else {
            vibrator.vibrate(longArrayOf(0, 1000, 500, 1000, 500, 2000), 0)
        }

        // Звук (зацикленный)
        val mediaPlayer = MediaPlayer.create(context, R.raw.alarm_sound).apply {
            isLooping = true
            start()
        }
        AlarmStopReceiver.mediaPlayer = mediaPlayer

        // Кнопка "Выключить"
        val stopIntent = Intent(context, AlarmStopReceiver::class.java).apply {
            putExtra("alarm_id", alarmId)
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            context, alarmId, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Кнопка "Отложить"
        val snoozeIntent = Intent(context, SnoozeReceiver::class.java).apply {
            putExtra("alarm_id", alarmId)
            putExtra("alarm_hour", hour)
            putExtra("alarm_minute", minute)
            putExtra("alarm_label", label)
            putExtra("alarm_shift_type", shiftType.name)
            putExtra("alarm_shift_cycle", shiftCycle)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context, alarmId + 10000, snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Полноэкранный Intent для пробуждения экрана (без FLAG_TURN_SCREEN_ON, так как WakeLock уже включает экран)
        val fullScreenIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("stop_alarm", true)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context, 0, fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Обычный Intent для открытия приложения
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("stop_alarm", true)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context, 0, mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        createNotificationChannel(context)

        val notification = NotificationCompat.Builder(context, "ALARM_CHANNEL")
            .setContentTitle("Будильник $label")
            .setContentText("Время ${String.format("%02d:%02d", hour, minute)}")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Выключить", stopPendingIntent)
            .addAction(android.R.drawable.ic_menu_recent_history, "Отложить", snoozePendingIntent)
            .setOngoing(true)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(alarmId, notification)

        // Перепланируем исходный будильник на следующий подходящий день
        val alarm = Alarm(
            id = alarmId, hour = hour, minute = minute, label = label,
            shiftType = shiftType, shiftCycle = shiftCycle, isEnabled = true
        )
        AlarmScheduler(context).scheduleAlarm(alarm)
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "ALARM_CHANNEL",
                "Будильники",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Канал будильника"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 500, 1000)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                setShowBadge(true)
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}