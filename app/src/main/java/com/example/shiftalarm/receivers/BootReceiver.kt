package com.example.shiftalarm.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.shiftalarm.utils.AlarmScheduler

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            Log.d("AlarmSched", "Телефон перезагружен! Восстанавливаем расписание сменных будильников...")

            // Восстановление всех сохраненных будильников
            AlarmScheduler(context).rescheduleAllAlarms()
        }
    }
}