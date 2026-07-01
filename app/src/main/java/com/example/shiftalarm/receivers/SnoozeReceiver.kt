package com.example.shiftalarm.receivers

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.util.Calendar

class SnoozeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getIntExtra("alarm_id", -1)
        val label = intent.getStringExtra("label") ?: "Будильник"

        Log.d("AlarmSched", "Ресивер SnoozeReceiver принял команду откладывания для ID: $alarmId")

        // 1. Принудительно закрываем текущее активное уведомление будильника
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(alarmId + 1000)

        // 2. Рассчитываем время сдвига: текущее время + 10 минут
        val snoozeCalendar = Calendar.getInstance().apply {
            add(Calendar.MINUTE, 10)
        }

        // 3. Создаем интент, который через 10 минут снова запустит AlarmReceiver и включит звук
        // Используем динамический поиск класса, чтобы избежать ошибок циклического импорта
        val targetClass = try {
            Class.forName("com.example.shiftalarm.receivers.AlarmReceiver")
        } catch (e: Exception) {
            Class.forName("com.example.shiftalarm.AlarmReceiver")
        }

        val alarmIntent = Intent(context, targetClass).apply {
            putExtra("alarm_id", alarmId)
            putExtra("label", "$label (Отложенный)")
            putExtra("shift_type", "ALL") // Отложенный будильник должен прозвенеть гарантированно
            putExtra("shift_cycle", 1)
            putExtra("start_date", "2026-05-20")
            putExtra("is_snooze", true)
        }

        // КРИТИЧЕСКИ ВАЖНО: requestCode равен (alarmId + 5000), чтобы не затереть основное расписание графиков смен!
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId + 5000,
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // 4. Планируем точный триггер в системе Android
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(snoozeCalendar.timeInMillis, pendingIntent),
                pendingIntent
            )
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, snoozeCalendar.timeInMillis, pendingIntent)
        }

        Log.d("AlarmSched", "Будильник ID $alarmId успешно отложен на 10 минут. Новый триггер: ${snoozeCalendar.time}")
    }
}