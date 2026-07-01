package com.example.shiftalarm.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class AlarmStorage(context: Context) {
    private val prefs = context.getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveAlarms(alarms: List<Alarm>) {
        val json = gson.toJson(alarms)
        prefs.edit().putString("alarms_list", json).apply()
    }

    fun getAlarms(): List<Alarm> {
        val json = prefs.getString("alarms_list", null) ?: return emptyList()
        val type = object : TypeToken<List<Alarm>>() {}.type
        return gson.fromJson(json, type)
    }

    fun updateAlarm(updatedAlarm: Alarm) {
        val list = getAlarms().toMutableList()
        val index = list.indexOfFirst { it.id == updatedAlarm.id }
        if (index != -1) {
            list[index] = updatedAlarm
            saveAlarms(list)
        }
    }
}