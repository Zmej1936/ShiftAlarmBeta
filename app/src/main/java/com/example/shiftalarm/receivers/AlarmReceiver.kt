package com.example.shiftalarm.receivers

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.Vibrator
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.shiftalarm.MainActivity
import com.example.shiftalarm.data.Alarm
import com.example.shiftalarm.data.ShiftType
import com.example.shiftalarm.utils.AlarmScheduler

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getIntExtra("alarm_id", -1)
        if (alarmId == -1) {
            Log.e("AlarmReceiver", "Нет ID будильника")
            return
        }

        val isSnooze = intent.getBooleanExtra("is_snooze", false)

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

        if (!isSnooze) {
            val scheduler = AlarmScheduler(context)
            scheduler.scheduleAlarm(alarm, globalShiftCycle)
        }

        playAlarmSound(context)
        startVibration(context)
        showNotification(context, alarm, globalShiftCycle)

        scheduleAutoStop(context, alarm)
    }

    private fun scheduleAutoStop(context: Context, alarm: Alarm) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmStopReceiver::class.java).apply {
            putExtra("alarm_id", alarm.id)
            putExtra("auto_stop", true)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id + 3000,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val triggerTime = System.currentTimeMillis() + 90_000
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }
        Log.d("AlarmReceiver", "Auto-stop scheduled in 90 sec")
    }

    private fun playAlarmSound(context: Context) {
        val intent = Intent(context, AlarmSoundService::class.java)
        context.startService(intent)
    }

    private fun startVibration(context: Context) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(android.os.VibrationEffect.createOneShot(90_000, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(90_000)
        }
    }

    private fun showNotification(context: Context, alarm: Alarm, globalShiftCycle: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "alarm_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Будильники",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Канал для сработавших будильников"
                enableVibration(true)
                setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), null)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("stop_alarm", true)
        }
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            addFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION)
        }
        val mainPendingIntent = PendingIntent.getActivity(
            context,
            alarm.id,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val disableIntent = Intent(context, AlarmStopReceiver::class.java).apply {
            putExtra("alarm_id", alarm.id)
            putExtra("alarm_hour", alarm.hour)
            putExtra("alarm_minute", alarm.minute)
            putExtra("alarm_label", alarm.label)
            putExtra("alarm_shift_type", alarm.shiftType.name)
            putExtra("alarm_shift_cycle", globalShiftCycle)
            putExtra("alarm_enabled", alarm.isEnabled)
            putExtra("user_action", "disable")
        }
        val disablePendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id,
            disableIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, SnoozeReceiver::class.java).apply {
            putExtra("alarm_id", alarm.id)
            putExtra("alarm_hour", alarm.hour)
            putExtra("alarm_minute", alarm.minute)
            putExtra("alarm_label", alarm.label)
            putExtra("alarm_shift_type", alarm.shiftType.name)
            putExtra("alarm_shift_cycle", globalShiftCycle)
            putExtra("alarm_enabled", alarm.isEnabled)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id + 1000,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle("Сменный будильник")
            .setContentText("${alarm.label} — ${String.format("%02d:%02d", alarm.hour, alarm.minute)}")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setFullScreenIntent(mainPendingIntent, true)
            .setAutoCancel(false)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Отключить", disablePendingIntent)
            .addAction(android.R.drawable.ic_menu_send, "Отложить", snoozePendingIntent)
            .build()

        notificationManager.notify(alarm.id, notification)
    }
}