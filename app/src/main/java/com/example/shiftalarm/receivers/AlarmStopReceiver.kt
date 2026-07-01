package com.example.shiftalarm.receivers

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer

class AlarmStopReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getIntExtra("alarm_id", -1)

        // 1. Гасим и останавливаем Foreground-сервис звука
        val serviceIntent = Intent(context, AlarmSoundService::class.java)
        context.stopService(serviceIntent)

        // 2. Дополнительно высвобождаем статический плеер, если он остался активен
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 3. Удаляем уведомление из шторки
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (alarmId != -1) {
            notificationManager.cancel(alarmId + 1000) // ID из startForeground
        }
    }

    companion object {
        var mediaPlayer: MediaPlayer? = null
    }
}