package com.example.shiftalarm.receivers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import com.example.shiftalarm.MainActivity
import com.example.shiftalarm.R
import java.util.Locale

class AlarmSoundService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val alarmId = intent?.getIntExtra("alarm_id", -1) ?: -1
        val label = intent?.getStringExtra("label") ?: "Будильник"
        val hour = intent?.getIntExtra("hour", 8) ?: 8
        val minute = intent?.getIntExtra("minute", 0) ?: 0

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ShiftAlarm::WakeLockTag").apply {
            acquire(10 * 60 * 1000L)
        }

        // КРИТИЧЕСКОЕ ИСПРАВЛЕНИЕ: Сначала принудительно регистрируем Foreground-службу в системе Android
        startForegroundServiceWithNotification(label, alarmId, hour, minute)

        startPlayback()
        startVibration()

        val lockScreenIntent = Intent(this, MainActivity::class.java).apply {
            putExtra("is_alarm_trigger", true)
            putExtra("alarm_id", alarmId)
            putExtra("label", label)
            putExtra("hour", hour)
            putExtra("minute", minute)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(lockScreenIntent)

        return START_STICKY
    }

    private fun startPlayback() {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer.create(this, R.raw.alarm_sound).apply {
                isLooping = true
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startVibration() {
        try {
            vibrator = getSystemService("vibrator") as Vibrator
            vibrator?.let {
                if (it.hasVibrator()) {
                    val pattern = longArrayOf(0, 500, 500, 500)
                    @Suppress("DEPRECATION")
                    it.vibrate(pattern, 0)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startForegroundServiceWithNotification(label: String, alarmId: Int, hour: Int, minute: Int) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("alarm_channel", "Будильник", NotificationManager.IMPORTANCE_HIGH).apply {
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                enableVibration(true)
                setBypassDnd(true)
            }
            manager.createNotificationChannel(channel)
        }

        val mainIntent = Intent(this, MainActivity::class.java).apply {
            putExtra("is_alarm_trigger", true)
            putExtra("alarm_id", alarmId)
            putExtra("label", label)
            putExtra("hour", hour)
            putExtra("minute", minute)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val mainPendingIntent = PendingIntent.getActivity(
            this, alarmId, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(this, SnoozeReceiver::class.java).apply {
            putExtra("alarm_id", alarmId)
            putExtra("label", label)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            this, alarmId + 5000, snoozeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, AlarmStopReceiver::class.java).apply {
            putExtra("alarm_id", alarmId)
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            this, alarmId + 6000, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, "alarm_channel")
            .setContentTitle(label)
            .setContentText("Будильник работает!")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(mainPendingIntent)
            .setFullScreenIntent(mainPendingIntent, true)
            .addAction(android.R.drawable.ic_menu_recent_history, "Отложить 10 мин", snoozePendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Отключить", stopPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)

        val notification = notificationBuilder.build()
        notification.flags = notification.flags or NotificationCompat.FLAG_INSISTENT

        // ИСПРАВЛЕНО ДЛЯ ANDROID 14: Привязываем уведомление к Foreground-режиму
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(alarmId + 1000, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(alarmId + 1000, notification)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            if (wakeLock?.isHeld == true) wakeLock?.release()
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            vibrator?.cancel()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}