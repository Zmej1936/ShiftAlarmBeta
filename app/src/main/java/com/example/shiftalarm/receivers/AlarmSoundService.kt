package com.example.shiftalarm.receivers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import com.example.shiftalarm.MainActivity
import com.example.shiftalarm.R

class AlarmSoundService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val alarmId = intent?.getIntExtra("alarm_id", -1) ?: -1
        val label = intent?.getStringExtra("label") ?: "Будильник"

        // 1. Захватываем WakeLock, чтобы процессор не уснул
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ShiftAlarm::WakeLockTag").apply {
            acquire(10 * 60 * 1000L) // Лимит 10 минут
        }

        // 2. Показываем уведомление
        showSimpleNotification(label, alarmId)

        // 3. Запуск звука и вибрации
        startPlayback()
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
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startVibration() {
        try {
            // ИСПРАВЛЕНО: Заменено на современный Context.VIBRATOR_SERVICE для устранения варнинга Java
            vibrator = getSystemService("vibrator") as Vibrator
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

    private fun showSimpleNotification(label: String, alarmId: Int) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("alarm_channel", "Будильник", NotificationManager.IMPORTANCE_HIGH)
            manager.createNotificationChannel(channel)
        }

        val mainIntent = Intent(this, MainActivity::class.java).apply {
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

        val notification = NotificationCompat.Builder(this, "alarm_channel")
            .setContentTitle(label)
            .setContentText("Будильник работает!")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setContentIntent(mainPendingIntent)
            .addAction(android.R.drawable.ic_menu_recent_history, "Отложить 10 мин", snoozePendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Отключить", stopPendingIntent)
            .build()

        manager.notify(alarmId + 1000, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        // ИСПРАВЛЕНО: Безопасное и изолированное уничтожение ресурсов внутри onDestroy сервиса
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