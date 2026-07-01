package com.example.shiftalarm.viewmodel

import android.app.Application
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

    // Подписываем UI напрямую к потоку нашего реактивного репозитория
    val alarms: StateFlow<List<Alarm>> = AlarmRepository.alarmsFlow

    // Получение конкретного будильника по ID для экрана редактирования
    fun getAlarmById(id: Int): kotlinx.coroutines.flow.Flow<Alarm?> {
        return kotlinx.coroutines.flow.flow {
            val list = AlarmRepository.alarmsFlow.value
            emit(list.find { it.id == id })
        }
    }

    // ИСПРАВЛЕНО: Сохранение через JSON-репозиторий
    fun insertAlarm(alarm: Alarm) {
        viewModelScope.launch {
            val currentList = AlarmRepository.alarmsFlow.value.toMutableList()
            val index = currentList.indexOfFirst { it.id == alarm.id }
            if (index != -1) {
                currentList[index] = alarm
            } else {
                currentList.add(alarm)
            }
            AlarmRepository.saveAlarms(context, currentList)
            if (alarm.isEnabled) {
                scheduler.scheduleAlarm(alarm)
            }
        }
    }

    // ИСПРАВЛЕНО: Удаление через JSON-репозиторий
    fun deleteAlarm(alarm: Alarm) {
        viewModelScope.launch {
            scheduler.cancelAllForAlarm(alarm)
            val currentList = AlarmRepository.alarmsFlow.value.filter { it.id != alarm.id }
            AlarmRepository.saveAlarms(context, currentList)
        }
    }

    // ИСПРАВЛЕНО: Переключение тумблера активности
    fun updateAlarmEnabled(alarm: Alarm, isEnabled: Boolean) {
        viewModelScope.launch {
            val updatedAlarm = alarm.copy(isEnabled = isEnabled)
            if (isEnabled) {
                scheduler.scheduleAlarm(updatedAlarm)
            } else {
                scheduler.cancelAllForAlarm(alarm)
            }
            AlarmRepository.updateAlarm(context, updatedAlarm)
        }
    }
}