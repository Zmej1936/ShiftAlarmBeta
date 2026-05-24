package com.example.shiftalarm.receivers

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.shiftalarm.data.Alarm
import com.example.shiftalarm.data.ShiftType
import com.example.shiftalarm.utils.AlarmScheduler

class SnoozeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("SnoozeReceiver", "Snooze triggered")

        // Останавливаем сервис со звуком
        val soundIntent = Intent(context, AlarmSoundService::class.java)
        context.stopService(soundIntent)

        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
        vibrator.cancel()

        val alarmId = intent.getIntExtra("alarm_id", -1)
        if (alarmId == -1) {
            Log.e("SnoozeReceiver", "No alarm ID")
            return
        }

        cancelAutoStop(context, alarmId)

        val hour = intent.getIntExtra("alarm_hour", 0)
        val minute = intent.getIntExtra("alarm_minute", 0)
        val label = intent.getStringExtra("alarm_label") ?: "Будильник"
        val shiftTypeName = intent.getStringExtra("alarm_shift_type") ?: "WORK_DAY"
        val globalShiftCycle = intent.getIntExtra("alarm_shift_cycle", 2)
        val isEnabled = intent.getBooleanExtra("alarm_enabled", true)

        val shiftType = try {
            ShiftType.valueOf(shiftTypeName)
        } catch (e: Exception) {
            ShiftType.WORK_DAY
        }

        val alarm = Alarm(
            id = alarmId,
            hour = hour,
            minute = minute,
            label = label,
            shiftType = shiftType,
            shiftCycle = globalShiftCycle,
            isEnabled = isEnabled
        )

        val scheduler = AlarmScheduler(context)
        scheduler.cancelAllForAlarm(alarm)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(alarmId)

        scheduleSnooze(context, alarm)
    }

    private fun scheduleSnooze(context: Context, alarm: Alarm) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerTime = System.currentTimeMillis() + 5 * 60 * 1000

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("alarm_id", alarm.id)
            putExtra("alarm_hour", alarm.hour)
            putExtra("alarm_minute", alarm.minute)
            putExtra("alarm_label", alarm.label)
            putExtra("alarm_shift_type", alarm.shiftType.name)
            putExtra("alarm_shift_cycle", alarm.shiftCycle)
            putExtra("alarm_enabled", alarm.isEnabled)
            putExtra("is_snooze", true)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id + 2000,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }
        Log.d("SnoozeReceiver", "Snooze scheduled for ${java.util.Date(triggerTime)}")
    }

    private fun cancelAutoStop(context: Context, alarmId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmStopReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId + 3000,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        Log.d("SnoozeReceiver", "Auto-stop cancelled")
    }
}