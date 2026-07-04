package com.example.shiftalarm.data

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

class AlarmStorage(context: Context) {
    private val prefs = context.getSharedPreferences("shift_alarm_prefs", Context.MODE_PRIVATE)

    fun getAlarms(): List<Alarm> {
        val jsonString = prefs.getString("alarms_json", null) ?: return emptyList()
        val list = mutableListOf<Alarm>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    Alarm(
                        id = obj.getInt("id"),
                        hour = obj.getInt("hour"),
                        minute = obj.getInt("minute"),
                        label = obj.getString("label"),
                        isEnabled = obj.getBoolean("isEnabled"),
                        shiftCycle = obj.getInt("shiftCycle"),
                        startDate = obj.optString("startDate", "2026-05-20"),
                        shiftType = ShiftType.valueOf(obj.getString("shiftType")),
                        nextTriggerDateTime = obj.optString("nextTriggerDateTime", "")
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("AlarmStorage", "Ошибка чтения JSON: ${e.message}")
        }
        return list
    }

    fun saveAlarms(alarms: List<Alarm>) {
        val jsonArray = JSONArray()
        alarms.forEach { alarm ->
            val obj = JSONObject().apply {
                put("id", alarm.id)
                put("hour", alarm.hour)
                put("minute", alarm.minute)
                put("label", alarm.label)
                put("isEnabled", alarm.isEnabled)
                put("shiftCycle", alarm.shiftCycle)
                put("startDate", alarm.startDate)
                put("shiftType", alarm.shiftType.name)
                put("nextTriggerDateTime", alarm.nextTriggerDateTime)
            }
            jsonArray.put(obj)
        }
        // КРИТИЧЕСКОЕ ИСПРАВЛЕНИЕ: Используем commit() вместо apply(),
        // чтобы данные физически стерлись с диска мгновенно и без задержек потоков!
        prefs.edit().putString("alarms_json", jsonArray.toString()).commit()
        Log.d("AlarmStorage", "База данных JSON успешно синхронизирована. Записано: ${alarms.size}")
    }

    fun updateAlarm(alarm: Alarm) {
        val current = getAlarms().toMutableList()
        val index = current.indexOfFirst { it.id == alarm.id }
        if (index != -1) {
            current[index] = alarm
            saveAlarms(current)
        }
    }
}