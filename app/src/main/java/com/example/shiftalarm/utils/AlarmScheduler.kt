package com.example.shiftalarm.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.shiftalarm.data.Alarm
import com.example.shiftalarm.receivers.AlarmReceiver
import java.util.Calendar

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleAlarm(alarm: Alarm, globalShiftCycle: Int) {
        if (!alarm.isEnabled) {
            Log.d("AlarmScheduler", "Будильник ${alarm.label} выключен, не планируем")
            return
        }

        // Проверяем разрешение на точные будильники (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.e("AlarmScheduler", "Нет разрешения на точные будильники!")
                return
            }
        }

        val nextTriggerTime = findNextValidTriggerTime(alarm, globalShiftCycle)
        if (nextTriggerTime == null) {
            Log.e("AlarmScheduler", "Не удалось найти следующее время для будильника ${alarm.label}")
            return
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("alarm_id", alarm.id)
            putExtra("alarm_hour", alarm.hour)
            putExtra("alarm_minute", alarm.minute)
            putExtra("alarm_label", alarm.label)
            putExtra("alarm_shift_type", alarm.shiftType.name)
            putExtra("alarm_shift_cycle", globalShiftCycle)
            putExtra("alarm_enabled", alarm.isEnabled)
            putExtra("is_snooze", false)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        Log.d("AlarmScheduler", "Планирую будильник ${alarm.label} на ${java.util.Date(nextTriggerTime)}")

        // Основной метод для будильников
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(nextTriggerTime, pendingIntent),
                pendingIntent
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, nextTriggerTime, pendingIntent)
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, nextTriggerTime, pendingIntent)
        }
    }

    fun cancelAllForAlarm(alarm: Alarm) {
        cancelPendingIntent(alarm.id)          // основное расписание
        cancelPendingIntent(alarm.id + 1000)   // кнопка "Отложить"
        cancelPendingIntent(alarm.id + 2000)   // повторный отложенный звонок
        cancelPendingIntent(alarm.id + 3000)   // авто-остановка через 90 секунд
        Log.d("AlarmScheduler", "Отменены все PendingIntent для будильника ${alarm.id}")
    }

    private fun cancelPendingIntent(requestCode: Int) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private fun findNextValidTriggerTime(alarm: Alarm, shiftCycle: Int): Long? {
        val now = Calendar.getInstance()
        var checkCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= now.timeInMillis) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        var daysChecked = 0
        val maxDays = 60

        while (daysChecked < maxDays) {
            val year = checkCalendar.get(Calendar.YEAR)
            val month = checkCalendar.get(Calendar.MONTH) + 1
            val day = checkCalendar.get(Calendar.DAY_OF_MONTH)

            val shiftTypeForDate = ShiftCalculator.getShiftTypeForDate(year, month, day, shiftCycle)

            val shouldRing = when (alarm.shiftType) {
                com.example.shiftalarm.data.ShiftType.ALL -> true
                else -> shiftTypeForDate == alarm.shiftType
            }

            if (shouldRing) {
                return checkCalendar.timeInMillis
            }

            checkCalendar.add(Calendar.DAY_OF_YEAR, 1)
            daysChecked++
        }
        return null
    }
}