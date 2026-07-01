package com.example.shiftalarm.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
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

        // ДОБАВЛЕНО: Извлекаем индивидуальную дату старта графика (или берем дефолтную)
        val startDateStr = intent.getStringExtra("start_date") ?: "2026-05-20"

        Log.d("AlarmSched", "===> Ресивер принял триггер для ID: $alarmId ($label)")

        // ИСПРАВЛЕНО: Передаем извлеченную дату старта графика в калькулятор смен
        val shouldRing = ShiftCalculator.shouldAlarmRingToday(shiftType, shiftCycle, startDateStr)

        Log.d("AlarmSched", "Вердикт калькулятора смен (shouldRing): $shouldRing для даты старта $startDateStr")

        if (!shouldRing) {
            Log.d("AlarmSched", "Будильник пропущен, так как сегодня по графику выходной.")

            // Если сегодня выходной, перепланируем будильник дальше с учетом его даты старта
            val alarm = Alarm(alarmId, hour, minute, label, shiftType, shiftCycle, true, startDateStr)
            AlarmScheduler(context).scheduleAlarm(alarm)
            return
        }

        Log.d("AlarmSched", "Запускаем Foreground Service для проигрывания звука...")

        // Запуск Foreground Service для непрерывного воспроизведения звука и показа кнопок
        val serviceIntent = Intent(context, AlarmSoundService::class.java).apply {
            putExtra("alarm_id", alarmId)
            putExtra("label", label)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }

        // ИСПРАВЛЕНО: Перепланируем оригинальный будильник на следующий цикл с микро-задержкой в 5 секунд,
        // чтобы вызов PendingIntent внутри AlarmScheduler не прерывал инициализацию MediaPlayer в сервисе
        Handler(Looper.getMainLooper()).postDelayed({
            val alarm = Alarm(alarmId, hour, minute, label, shiftType, shiftCycle, true, startDateStr)
            AlarmScheduler(context).scheduleAlarm(alarm)
            Log.d("AlarmSched", "Будильник ID: $alarmId успешно перепланирован на следующий раз.")
        }, 5000)
    }
}