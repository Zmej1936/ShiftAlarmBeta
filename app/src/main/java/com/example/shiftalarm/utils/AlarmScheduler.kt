package com.example.shiftalarm.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import com.example.shiftalarm.data.Alarm
import com.example.shiftalarm.data.AlarmStorage
import java.util.Calendar
import java.util.Date

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private val targetReceiverClass: Class<*> by lazy {
        try {
            Class.forName("com.example.shiftalarm.receivers.AlarmReceiver")
        } catch (e: Exception) {
            Class.forName("com.example.shiftalarm.AlarmReceiver")
        }
    }

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

        val intent = Intent(context, targetReceiverClass).apply {
            data = Uri.parse("alarm://${alarm.id}")
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

    // УЛУЧШЕНО: Алгоритм тотальной инспекции и глубокой очистки памяти ОС Android
    fun rescheduleAllAlarms() {
        val storage = AlarmStorage(context)
        val activeAlarms = storage.getAlarms()
        val activeIds = activeAlarms.map { it.id }.toSet()

        Log.d("AlarmScheduler", "=== ГЕНЕРАЛЬНАЯ УБОРКА ПАМЯТИ ===")
        Log.d("AlarmScheduler", "Активных будильников в JSON: ${activeAlarms.size}")

        // 1. АЛГОРИТМ ОЧИСТКИ СЛЕДОВ СТАРЫХ ВЕРСИЙ (Без Data URI):
        // Пробегаем по пулу возможных старых ID от 0 до 100 и принудительно
        // стираем старые "слепые" интенты, которые могли остаться в памяти телефона
        for (oldId in 0..100) {
            try {
                // Создаем интент старого образца (без data = Uri.parse)
                val oldIntent = Intent(context, targetReceiverClass)

                // Ищем, есть ли такой старый триггер в системе (FLAG_NO_CREATE)
                val oldPending = PendingIntent.getBroadcast(
                    context, oldId, oldIntent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                )
                if (oldPending != null) {
                    alarmManager.cancel(oldPending)
                    oldPending.cancel()
                    Log.d("AlarmScheduler", "Выжжен застрявший ПРИЗРАК старой версии приложения с ID: $oldId")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 2. Планируем те будильники, которые реально есть в базе данных на экране
        activeAlarms.forEach { alarm ->
            if (alarm.isEnabled) {
                scheduleAlarm(alarm, forceNextDay = false)
            } else {
                cancelAllForAlarm(alarm)
            }
        }

        // 3. Дополнительная зачистка для новых типов интентов (с Data URI)
        // Если ID нет в файле JSON — стираем все его системные каналы
        for (testId in 0..100) {
            if (!activeIds.contains(testId)) {
                cancelAllForAlarmIdOnly(testId)
            }
        }
    }

    fun cancelAllForAlarm(alarm: Alarm) {
        cancelPendingIntent(alarm, alarm.id)
        cancelPendingIntent(alarm, alarm.id + 1000)
        cancelPendingIntent(alarm, alarm.id + 5000)
        Log.d("AlarmScheduler", "===> Системный AlarmManager ТОТАЛЬНО ВЫЖЕГ таймеры для ID: ${alarm.id}")
    }

    // Вспомогательный метод очистки по ID для незарегистрированных элементов
    private fun cancelAllForAlarmIdOnly(alarmId: Int) {
        val dummyAlarm = Alarm(
            id = alarmId, hour = 0, minute = 0, label = "", isEnabled = false,
            shiftCycle = 0, startDate = "2026-05-20",
            shiftType = com.example.shiftalarm.data.ShiftType.ALL, nextTriggerDateTime = ""
        )
        cancelPendingIntent(dummyAlarm, alarmId)
        cancelPendingIntent(dummyAlarm, alarmId + 1000)
        cancelPendingIntent(dummyAlarm, alarmId + 5000)
    }

    private fun cancelPendingIntent(alarm: Alarm, requestCode: Int) {
        val intent = Intent(context, targetReceiverClass).apply {
            data = Uri.parse("alarm://${alarm.id}")
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
            requestCode,
            intent,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d("AlarmScheduler", "Уничтожен системный PendingIntent для кода: $requestCode")
        }
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