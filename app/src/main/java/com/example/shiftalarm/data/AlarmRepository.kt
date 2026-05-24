package com.example.shiftalarm.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.shiftalarm.alarmsDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.io.IOException

class AlarmRepository(private val context: Context) {

    private val gson = Gson()
    private val alarmsKey = stringPreferencesKey("alarms_list")

    private fun fixAlarm(alarm: Alarm): Alarm {
        return alarm.copy(
            shiftType = alarm.shiftType ?: ShiftType.ALL,
            shiftCycle = if (alarm.shiftCycle <= 0) 2 else alarm.shiftCycle
        )
    }

    val alarmsFlow: Flow<List<Alarm>> = context.alarmsDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val json = preferences[alarmsKey] ?: "[]"
            val type = object : TypeToken<List<Alarm>>() {}.type
            val list: List<Alarm> = gson.fromJson(json, type)
            list.map { fixAlarm(it) }  // <---修复
        }

    suspend fun saveAlarms(alarms: List<Alarm>) {
        context.alarmsDataStore.edit { preferences ->
            preferences[alarmsKey] = gson.toJson(alarms)
        }
    }

    suspend fun insertAlarm(alarm: Alarm) {
        val currentList = getCurrentAlarms()
        val newList = currentList.toMutableList()
        val index = newList.indexOfFirst { it.id == alarm.id }
        if (index != -1) {
            newList[index] = alarm
        } else {
            newList.add(alarm)
        }
        saveAlarms(newList)
    }

    suspend fun deleteAlarm(alarm: Alarm) {
        val currentList = getCurrentAlarms()
        val newList = currentList.filter { it.id != alarm.id }
        saveAlarms(newList)
    }

    private suspend fun getCurrentAlarms(): List<Alarm> {
        return context.alarmsDataStore.data
            .map { preferences ->
                val json = preferences[alarmsKey] ?: "[]"
                val type = object : TypeToken<List<Alarm>>() {}.type
                val list: List<Alarm> = gson.fromJson(json, type)
                list.map { fixAlarm(it) }
            }
            .catch { emit(emptyList()) }
            .firstOrNull() ?: emptyList()
    }
}