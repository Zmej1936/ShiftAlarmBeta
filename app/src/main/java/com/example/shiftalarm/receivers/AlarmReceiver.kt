package com.example.shiftalarm.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.shiftalarm.data.Alarm
import com.example.shiftalarm.data.ShiftType
import com.example.shiftalarm.utils.AlarmScheduler
import com.example.shiftalarm.utils.ShiftCalculator

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getIntExtra("alarm_id", -1)
        val hour = intent.getIntExtra("hour", 0)
        val minute = intent.getIntExtra("minute", 0)
        val label = intent.getStringExtra("label") ?: "Будильник"
        val shiftTypeName = intent.getStringExtra("shift_type") ?: "ALL"
        val shiftCycle = intent.getIntExtra("shift_cycle", 2)
        val shiftType = try { ShiftType.valueOf(shiftTypeName) } catch (e: Exception) { ShiftType.ALL }
        val startDateStr = intent.getStringExtra("start_date") ?: "2026-05-20"

        Log.d("AlarmSched", "===> Ресивер принял триггер для ID: $alarmId ($label)")

        // 1. Проверяем, должен ли будильник звенеть сегодня по графику смен
        val shouldRing = ShiftCalculator.shouldAlarmRingToday(shiftType, shiftCycle, startDateStr)

        if (!shouldRing) {
            Log.d("AlarmSched", "Сегодня выходной. Молча перепланируем на следующий день.")
            // Если сегодня выходной, просто регистрируем триггер на следующий день в системе
            val alarm = Alarm(alarmId, hour, minute, label, shiftType, shiftCycle, true, startDateStr)
            AlarmScheduler(context).scheduleAlarm(alarm)
            return
        }

        // 2. Если сегодня рабочий день — запускаем проигрывание звука
        Log.d("AlarmSched", "Рабочий день! Включаем звук будильника.")
        val serviceIntent = Intent(context, AlarmSoundService::class.java).apply {
            putExtra("alarm_id", alarmId)
            putExtra("label", label)
        }
        context.startService(serviceIntent)

        // 3. ИСПРАВЛЕНО: Вместо временной задержки Handler, мы просто планируем следующий день.
        // AlarmManager обновит интент, а UI обновится, когда пользователь нажмет "Отключить".
        val alarm = Alarm(alarmId, hour, minute, label, shiftType, shiftCycle, true, startDateStr)
        AlarmScheduler(context).scheduleAlarm(alarm)
    }
}