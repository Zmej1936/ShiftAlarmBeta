package com.example.shiftalarm.receivers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.shiftalarm.data.Alarm
import com.example.shiftalarm.utils.AlarmScheduler
import java.util.Calendar

class SnoozeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("AlarmDebug", "Snooze triggered")

        val alarmId = intent.getIntExtra("alarm_id", 0)
        val hour = intent.getIntExtra("alarm_hour", 0)
        val minute = intent.getIntExtra("alarm_minute", 0)
        val label = intent.getStringExtra("alarm_label") ?: "Будильник"
        val shiftTypeName = intent.getStringExtra("alarm_shift_type") ?: "ALL"
        val shiftCycle = intent.getIntExtra("alarm_shift_cycle", 2)

        val shiftType = try {
            com.example.shiftalarm.data.ShiftType.valueOf(shiftTypeName)
        } catch (e: Exception) {
            com.example.shiftalarm.data.ShiftType.ALL
        }

        // Остановить текущий звук и вибрацию
        val stopIntent = Intent(context, AlarmStopReceiver::class.java)
        context.sendBroadcast(stopIntent)

        // Отменить текущий будильник
        val currentAlarm = Alarm(
            id = alarmId,
            hour = hour,
            minute = minute,
            label = label,
            shiftType = shiftType,
            shiftCycle = shiftCycle,
            isEnabled = true
        )
        val scheduler = AlarmScheduler(context)
        scheduler.cancelAlarm(currentAlarm)

        // Запланировать отложенный будильник через 10 минут
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MINUTE, 10)
        val snoozeTime = calendar.timeInMillis

        val snoozeIntent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("alarm_id", alarmId)
            putExtra("alarm_hour", calendar.get(Calendar.HOUR_OF_DAY))
            putExtra("alarm_minute", calendar.get(Calendar.MINUTE))
            putExtra("alarm_label", label)
            putExtra("alarm_shift_type", shiftType.name)
            putExtra("alarm_shift_cycle", shiftCycle)
            putExtra("is_snooze", true)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId + 10000,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(snoozeTime, null), snoozePendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, snoozeTime, snoozePendingIntent)
        }

        // Перепланировать исходный будильник на следующий подходящий день
        scheduler.scheduleAlarm(currentAlarm)

        Log.d("AlarmDebug", "Snooze scheduled at ${calendar.time}")
    }
}