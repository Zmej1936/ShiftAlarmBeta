package com.example.shiftalarm.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.shiftalarm.data.Alarm
import com.example.shiftalarm.data.AlarmStorage
import com.example.shiftalarm.receivers.AlarmReceiver
import java.util.Calendar
import java.util.Date

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleAlarm(alarm: Alarm, forceNextDay: Boolean = false) {
        if (!alarm.isEnabled) {
            Log.d("AlarmScheduler", "Будильник ${alarm.label} выключен, не планируем")
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.e("AlarmScheduler", "Нет разрешения на точные будильники!")
                return
            }
        }

        val nextTriggerTime = findNextValidTriggerTime(alarm, forceNextDay)
        if (nextTriggerTime == null) {
            Log.e("AlarmScheduler", "Не удалось найти следующее время для будильника ${alarm.label}")
            return
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("alarm_id", alarm.id)
            putExtra("hour", alarm.hour)
            putExtra("minute", alarm.minute)
            putExtra("label", alarm.label)
            putExtra("shift_type", alarm.shiftType.name)
            putExtra("shift_cycle", alarm.shiftCycle)
            putExtra("start_date", alarm.startDate ?: "2026-05-20")
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val sdfText = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault())
        alarm.nextTriggerDateTime = sdfText.format(Date(nextTriggerTime))

        Log.d("AlarmScheduler", "Планирую будильник ${alarm.label} на ${Date(nextTriggerTime)}")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(nextTriggerTime, pendingIntent),
                pendingIntent
            )
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, nextTriggerTime, pendingIntent)
        }
    }

    // ВОССТАНОВЛЕНО: Метод принудительного массового перепланирования всех будильников
    fun rescheduleAllAlarms() {
        val storage = AlarmStorage(context)
        val alarms = storage.getAlarms()
        Log.d("AlarmScheduler", "Запущено фоновое обновление лимитов для ${alarms.size} будильников")
        alarms.forEach { alarm ->
            if (alarm.isEnabled) {
                scheduleAlarm(alarm, forceNextDay = false)
                storage.updateAlarm(alarm)
            }
        }
    }

    fun cancelAllForAlarm(alarm: Alarm) {
        cancelPendingIntent(alarm.id)
        cancelPendingIntent(alarm.id + 1000)
        cancelPendingIntent(alarm.id + 2000)
        cancelPendingIntent(alarm.id + 3000)
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

    private fun findNextValidTriggerTime(alarm: Alarm, forceNextDay: Boolean): Long? {
        val now = Calendar.getInstance()
        val checkCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            if (forceNextDay || timeInMillis <= now.timeInMillis) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        var daysChecked = 0
        val maxDays = 60
        val startDateStr = alarm.startDate ?: "2026-05-20"

        while (daysChecked < maxDays) {
            val year = checkCalendar.get(Calendar.YEAR)
            val month = checkCalendar.get(Calendar.MONTH) + 1
            val day = checkCalendar.get(Calendar.DAY_OF_MONTH)

            val shiftTypeForDate = ShiftCalculator.getShiftTypeForDate(
                year, month, day, alarm.shiftCycle, startDateStr
            )

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