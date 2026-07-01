package com.example.shiftalarm.receivers

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.shiftalarm.data.AlarmStorage
import com.example.shiftalarm.data.AlarmRepository
import com.example.shiftalarm.utils.AlarmScheduler

class AlarmStopReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getIntExtra("alarm_id", -1)
        Log.d("AlarmSched", "Нажата кнопка ОТКЛЮЧИТЬ для ID: $alarmId")

        context.stopService(Intent(context, AlarmSoundService::class.java))

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(alarmId + 1000)

        val storage = AlarmStorage(context)
        val alarms = storage.getAlarms()
        val alarm = alarms.find { it.id == alarmId }

        alarm?.let { currentAlarm ->
            val scheduler = AlarmScheduler(context)
            scheduler.scheduleAlarm(currentAlarm, forceNextDay = true)

            storage.updateAlarm(currentAlarm)
            AlarmRepository.updateAlarm(context, currentAlarm)
            Log.d("AlarmSched", "Будильник перенесен на следующую дату графика: ${currentAlarm.nextTriggerDateTime}")

            // Защита от засыпания: перепланируем расписание при контакте с уведомлением
            scheduler.rescheduleAllAlarms()
            Log.d("AlarmSched", "Системные таймеры AlarmManager обновлены при отключении звонка.")
        }
    }
}