package com.example.shiftalarm.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.shiftalarm.utils.AlarmScheduler
import com.example.shiftalarm.data.Alarm
import com.example.shiftalarm.data.ShiftType
import java.util.Calendar

class SnoozeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getIntExtra("alarm_id", -1)
        val label = intent.getStringExtra("label") ?: "Будильник"

        // 1. Полностью выключаем сервис звука и вибрации
        val serviceIntent = Intent(context, AlarmSoundService::class.java)
        context.stopService(serviceIntent)

        // 2. Рассчитываем точное время +10 минут от текущего момента
        val calendar = Calendar.getInstance().apply {
            add(Calendar.MINUTE, 10)
        }

        // 3. Создаем временный будильник-отложку
        val alarm = Alarm(
            id = alarmId,
            hour = calendar.get(Calendar.HOUR_OF_DAY),
            minute = calendar.get(Calendar.MINUTE),
            label = "$label (Отложен)",
            shiftType = ShiftType.ALL, // Чтобы сработал железно вне зависимости от графика смен
            shiftCycle = 1,
            isEnabled = true
        )

        AlarmScheduler(context).scheduleAlarm(alarm)
    }
}