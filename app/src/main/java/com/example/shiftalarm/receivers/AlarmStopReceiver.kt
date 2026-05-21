package com.example.shiftalarm.receivers

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Vibrator
import android.util.Log

class AlarmStopReceiver : BroadcastReceiver() {

    companion object {
        var mediaPlayer: MediaPlayer? = null
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("AlarmDebug", "AlarmStopReceiver triggered")

        // Остановка звука
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null

        // Остановка вибрации
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        vibrator.cancel()

        // Отмена уведомления
        val alarmId = intent.getIntExtra("alarm_id", 0)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(alarmId)

        // Освобождение wakeLock
        AlarmReceiver.wakeLock?.release()
        AlarmReceiver.wakeLock = null
    }
}