package com.example.shiftalarm.receivers

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.shiftalarm.data.AlarmStorage
import com.example.shiftalarm.utils.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmStopReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getIntExtra("alarm_id", -1)
        Log.d("AlarmSched", "AlarmStopReceiver: Команда ОТКЛЮЧИТЬ для ID: $alarmId")

        // 1. Мгновенно глушим фоновую службу звука и вибрации
        val serviceIntent = Intent(context, AlarmSoundService::class.java)
        context.stopService(serviceIntent)

        // 2. Закрываем активную шторку системного уведомления
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(alarmId + 1000)

        if (alarmId == -1) return

        // Использование goAsync() для безопасного выполнения асинхронных операций в BroadcastReceiver
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val storage = AlarmStorage(context)
                val scheduler = AlarmScheduler(context)
                val currentAlarms = storage.getAlarms()

                // Ищем в JSON строго ОДИН сработавший будильник
                val activeAlarm = currentAlarms.find { it.id == alarmId }

                // КРИТИЧЕСКОЕ ИСПРАВЛЕНИЕ: Мы БОЛЬШЕ НЕ вызываем scheduler.rescheduleAllAlarms().
                // Пересчитываем строго ОДИН этот активный будильник на следующий день сменного графика.
                // Если будильник был удален пользователем, activeAlarm вернет null, и вечный цикл воскрешения не запустится!
                if (activeAlarm != null && activeAlarm.isEnabled) {
                    scheduler.scheduleAlarm(activeAlarm, forceNextDay = true)
                    storage.updateAlarm(activeAlarm)
                    Log.d("AlarmSched", "Будильник ID $alarmId успешно пересчитан на следующий цикл.")
                } else {
                    Log.d("AlarmSched", "Будильник ID $alarmId не найден в базе или выключен. Пропускаем пересчет.")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish() // Обязательно завершаем асинхронный процесс ресивера
            }
        }
    }
}