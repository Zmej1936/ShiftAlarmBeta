package com.example.shiftalarm.receivers

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Vibrator
import android.util.Log
import com.example.shiftalarm.utils.AlarmScheduler

class AlarmStopReceiver : BroadcastReceiver() {

    companion object {
        var mediaPlayer: MediaPlayer? = null
    }

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getIntExtra("alarm_id", -1)
        val isAutoStop = intent.getBooleanExtra("auto_stop", false)
        val userAction = intent.getStringExtra("user_action")

        Log.d("AlarmStopReceiver", "Остановка звонка: autoStop=$isAutoStop, userAction=$userAction")

        // Останавливаем сервис со звуком
        val soundIntent = Intent(context, AlarmSoundService::class.java)
        context.stopService(soundIntent)

        // Останавливаем вибрацию
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        vibrator.cancel()

        // Отменяем запланированную автоматическую остановку (если есть)
        cancelAutoStop(context, alarmId)

        if (alarmId == -1) return

        // Удаляем уведомление
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(alarmId)

        // Важно: не отключаем будильник! Только останавливаем текущий звонок.
        // Будильник остаётся включённым и сработает в следующий рабочий/выходной/каждый день по расписанию
        Log.d("AlarmStopReceiver", "Будильник остановлен (звук и вибрация прекращены), сам будильник не отключён")
    }

    private fun cancelAutoStop(context: Context, alarmId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmStopReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId + 3000,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        Log.d("AlarmStopReceiver", "Auto-stop cancelled")
    }
}