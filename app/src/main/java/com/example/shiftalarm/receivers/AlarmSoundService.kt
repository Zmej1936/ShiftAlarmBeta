package com.example.shiftalarm.receivers

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import com.example.shiftalarm.MainActivity
import com.example.shiftalarm.R

class AlarmSoundService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val alarmId = intent?.getIntExtra("alarm_id", -1) ?: -1
        val label = intent?.getStringExtra("label") ?: "Будильник"

        // 1. Срочно запускаем Foreground-уведомление, чтобы систему не убил Android
        val notification = createForegroundNotification(label, alarmId)
        startForeground(alarmId + 1000, notification)

        // 2. Безопасный запуск музыки на бесконечный повтор
        startPlayback()

        // 3. Безопасный запуск вибрации
        startVibration()

        return START_STICKY
    }

    private fun startPlayback() {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer.create(this, R.raw.alarm_sound).apply {
                isLooping = true
                start()
            }
            // Сохраняем ссылку для статического доступа (если это используется в AlarmStopReceiver)
            AlarmStopReceiver.mediaPlayer = mediaPlayer
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startVibration() {
        try {
            vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibrator?.let {
                if (it.hasVibrator()) {
                    val pattern = longArrayOf(0, 500, 500, 500)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        it.vibrate(VibrationEffect.createWaveform(pattern, 0))
                    } else {
                        @Suppress("DEPRECATION")
                        it.vibrate(pattern, 0)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createForegroundNotification(label: String, alarmId: Int): android.app.Notification {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel("alarm_channel", "Будильник", android.app.NotificationManager.IMPORTANCE_HIGH)
            manager.createNotificationChannel(channel)
        }

        val mainIntent = Intent(this, com.example.shiftalarm.MainActivity::class.java).apply {
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

        return NotificationCompat.Builder(this, "alarm_channel")
            .setContentTitle(label)
            .setContentText("Будильник вовсю работает!")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            // Убрали setFullScreenIntent, так как для shortService на некоторых Android 14+
            // это может вызывать транзакционный сбой Binder до прорисовки окна
            .setContentIntent(mainPendingIntent)
            .addAction(android.R.drawable.ic_menu_recent_history, "Отложить 10 мин", snoozePendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Отключить", stopPendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            AlarmStopReceiver.mediaPlayer = null
        } catch (e: Exception) { e.printStackTrace() }

        try {
            vibrator?.cancel()
        } catch (e: Exception) { e.printStackTrace() }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}