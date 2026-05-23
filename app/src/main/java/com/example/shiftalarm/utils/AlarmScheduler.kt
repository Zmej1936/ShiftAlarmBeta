package com.example.shiftalarm.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.shiftalarm.data.Alarm
import com.example.shiftalarm.data.ShiftType
import com.example.shiftalarm.receivers.AlarmReceiver
import java.util.Calendar
import com.example.shiftalarm.utils.ShiftCalculator.getShiftTypeForDate

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleAlarm(alarm: Alarm) {
        if (!alarm.isEnabled) return

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("alarm_id", alarm.id)
            putExtra("alarm_hour", alarm.hour)
            putExtra("alarm_minute", alarm.minute)
            putExtra("alarm_label", alarm.label)
            putExtra("alarm_shift_type", alarm.shiftType.name)
            putExtra("alarm_shift_cycle", alarm.shiftCycle)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nextTriggerTime = findNextValidTriggerTime(alarm)
        if (nextTriggerTime != null) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                nextTriggerTime,
                pendingIntent
            )
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
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private fun findNextValidTriggerTime(alarm: Alarm): Long? {
        var daysToAdd = 0
        val now = Calendar.getInstance()
        val baseCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= now.timeInMillis) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        while (daysToAdd <= 30) {
            val checkCalendar = baseCalendar.clone() as Calendar
            checkCalendar.add(Calendar.DAY_OF_YEAR, daysToAdd)

            val year = checkCalendar.get(Calendar.YEAR)
            val month = checkCalendar.get(Calendar.MONTH)
            val day = checkCalendar.get(Calendar.DAY_OF_MONTH)

            val shiftTypeForDate = ShiftCalculator.getShiftTypeForDate(year, month + 1, day, alarm.shiftCycle)

            val shouldRing = if (alarm.shiftType == ShiftType.ALL) true else shiftTypeForDate == alarm.shiftType

            if (shouldRing) {
                return checkCalendar.timeInMillis
            }

            daysToAdd++
        }
        return null
    }
}