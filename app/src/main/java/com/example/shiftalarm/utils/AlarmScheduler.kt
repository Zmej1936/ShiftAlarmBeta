package com.example.shiftalarm.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.shiftalarm.data.Alarm
import com.example.shiftalarm.receivers.AlarmReceiver
import java.text.SimpleDateFormat
import java.util.*

class AlarmScheduler(private val context: Context) {

    fun scheduleAlarm(alarm: Alarm) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("alarm_id", alarm.id)
            putExtra("hour", alarm.hour)
            putExtra("minute", alarm.minute)
            putExtra("label", alarm.label)
            putExtra("shift_type", alarm.shiftType.name)
            putExtra("shift_cycle", alarm.shiftCycle)
            // Безопасно передаем дату старта, подставляя дефолтную, если она null
            putExtra("start_date", alarm.startDate ?: "2026-05-20")
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // ИСПРАВЛЕНО: Добавлен оператор ?: "2026-05-20" для защиты от ошибки типов Котлина
        val nextTime = ShiftCalculator.getNextAlarmDateTime(
            alarm.hour,
            alarm.minute,
            alarm.shiftType,
            alarm.shiftCycle,
            alarm.startDate ?: "2026-05-20"
        )

        if (nextTime != null) {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            Log.d("AlarmSched", "Планируем будильник ID: ${alarm.id} на время: ${sdf.format(nextTime.time)}")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextTime.timeInMillis, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, nextTime.timeInMillis, pendingIntent)
            }
        } else {
            Log.e("AlarmSched", "Ошибка: Калькулятор смен вернул null для будильника ID: ${alarm.id}")
        }
    }

    fun cancelAlarm(alarm: Alarm) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(pendingIntent)
    }
}