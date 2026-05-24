package com.example.shiftalarm.receivers

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.shiftalarm.R

class AlarmSoundService : Service() {
    private var mediaPlayer: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val NOTIFICATION_ID = 999
    private val CHANNEL_ID = "alarm_sound_channel"

    override fun onCreate() {
        super.onCreate()
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AlarmSoundService::lock")
        wakeLock?.acquire(10 * 60 * 1000L) // 10 минут
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())

        try {
            mediaPlayer = MediaPlayer().apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                } else {
                    @Suppress("DEPRECATION")
                    setAudioStreamType(android.media.AudioManager.STREAM_ALARM)
                }

                val afd = resources.openRawResourceFd(R.raw.alarm_sound)
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()

                prepare()
                isLooping = true

                setOnErrorListener { _, what, extra ->
                    Log.e("AlarmSoundService", "MediaPlayer error: what=$what, extra=$extra")
                    stopSelf()
                    false
                }
                setOnCompletionListener {
                    stopSelf()
                }
            }
            mediaPlayer?.start()
            Log.d("AlarmSoundService", "Воспроизведение запущено")
        } catch (e: Exception) {
            Log.e("AlarmSoundService", "Ошибка инициализации MediaPlayer", e)
            stopSelf()
        }

        AlarmStopReceiver.mediaPlayer = mediaPlayer
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Звук будильника",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Канал для воспроизведения звука будильника"
                setSound(null, null)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Будильник")
            .setContentText("Звонок...")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
    }

    override fun onDestroy() {
        try {
            mediaPlayer?.let { mp ->
                if (mp.isPlaying) {
                    mp.stop()
                }
                mp.release()
            }
        } catch (e: Exception) {
            Log.e("AlarmSoundService", "Ошибка при остановке MediaPlayer: ${e.message}")
        } finally {
            mediaPlayer = null
        }

        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                }
            }
        } catch (e: Exception) {
            Log.e("AlarmSoundService", "Ошибка при освобождении WakeLock: ${e.message}")
        } finally {
            wakeLock = null
        }

        Log.d("AlarmSoundService", "Сервис остановлен, ресурсы освобождены")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}