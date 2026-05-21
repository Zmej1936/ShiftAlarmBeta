package com.example.shiftalarm.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.shiftalarm.data.Alarm
import com.example.shiftalarm.data.AlarmRepository
import com.example.shiftalarm.utils.AlarmScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AlarmViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AlarmRepository(application)
    private val alarmScheduler = AlarmScheduler(application)

    val alarms: StateFlow<List<Alarm>> = repository.alarmsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _selectedAlarm = MutableStateFlow<Alarm?>(null)
    val selectedAlarm: StateFlow<Alarm?> = _selectedAlarm

    fun getAlarmById(id: Int) {
        viewModelScope.launch {
            _selectedAlarm.value = alarms.value.find { it.id == id }
        }
    }

    fun clearSelectedAlarm() {
        _selectedAlarm.value = null
    }

    fun insertAlarm(alarm: Alarm) {
        viewModelScope.launch {
            repository.insertAlarm(alarm)
            if (alarm.isEnabled) {
                alarmScheduler.scheduleAlarm(alarm)
            }
        }
    }

    fun deleteAlarm(alarm: Alarm) {
        viewModelScope.launch {
            alarmScheduler.cancelAlarm(alarm)
            repository.deleteAlarm(alarm)
        }
    }

    fun updateAlarmEnabled(alarm: Alarm, enabled: Boolean) {
        viewModelScope.launch {
            val updated = alarm.copy(isEnabled = enabled)
            repository.insertAlarm(updated)
            if (enabled) {
                alarmScheduler.scheduleAlarm(updated)
            } else {
                alarmScheduler.cancelAlarm(updated)
            }
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AlarmViewModel::class.java)) {
                return AlarmViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}