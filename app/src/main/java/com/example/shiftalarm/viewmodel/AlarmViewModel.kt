package com.example.shiftalarm.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.shiftalarm.data.Alarm
import com.example.shiftalarm.data.AlarmRepository
import com.example.shiftalarm.utils.AlarmScheduler
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AlarmViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val scheduler = AlarmScheduler(context)

    val alarms: StateFlow<List<Alarm>> = AlarmRepository.alarmsFlow

    fun getAlarmById(id: Int): kotlinx.coroutines.flow.Flow<Alarm?> {
        return kotlinx.coroutines.flow.flow {
            val list = AlarmRepository.alarmsFlow.value
            emit(list.find { it.id == id })
        }
    }

    fun insertAlarm(alarm: Alarm) {
        viewModelScope.launch {
            val currentList = AlarmRepository.alarmsFlow.value.toMutableList()
            val index = currentList.indexOfFirst { it.id == alarm.id }
            if (index != -1) {
                currentList[index] = alarm
            } else {
                currentList.add(alarm)
            }
            scheduler.cancelAllForAlarm(alarm)

            AlarmRepository.saveAlarms(context, currentList)
            if (alarm.isEnabled) {
                scheduler.scheduleAlarm(alarm)
            }
        }
    }

    fun deleteAlarm(alarm: Alarm) {
        viewModelScope.launch {
            android.util.Log.d("AlarmSched", "AlarmViewModel: Запрос на удаление будильника с ID: ${alarm.id}")

            // ИСПРАВЛЕНО: try/catch и переменные приведены к нижнему регистру
            try {
                val serviceIntent = Intent(context, Class.forName("com.example.shiftalarm.receivers.AlarmSoundService"))
                context.stopService(serviceIntent)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            scheduler.cancelAllForAlarm(alarm)

            val currentList = AlarmRepository.alarmsFlow.value.filter { it.id != alarm.id }
            AlarmRepository.saveAlarms(context, currentList)
        }
    }

    fun updateAlarmEnabled(alarm: Alarm, isEnabled: Boolean) {
        viewModelScope.launch {
            val updatedAlarm = alarm.copy(isEnabled = isEnabled)
            if (isEnabled) {
                scheduler.scheduleAlarm(updatedAlarm)
            } else {
                scheduler.cancelAllForAlarm(updatedAlarm)
            }
            AlarmRepository.updateAlarm(context, updatedAlarm)
        }
    }
}