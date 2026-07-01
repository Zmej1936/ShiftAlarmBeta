package com.example.shiftalarm.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.shiftalarm.data.Alarm
import com.example.shiftalarm.data.AlarmRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AlarmViewModel(private val repository: AlarmRepository) : ViewModel() {
    val alarms: StateFlow<List<Alarm>> = repository.alarmsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun insertAlarm(alarm: Alarm) = viewModelScope.launch { repository.insertAlarm(alarm) }
    fun deleteAlarm(alarm: Alarm) = viewModelScope.launch { repository.deleteAlarm(alarm) }
    fun updateAlarmEnabled(alarm: Alarm, enabled: Boolean) =
        viewModelScope.launch { repository.insertAlarm(alarm.copy(isEnabled = enabled)) }

    fun getAlarmById(id: Int): StateFlow<Alarm?> =
        alarms.map { list -> list.find { it.id == id } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
}

class AlarmViewModelFactory(private val repository: AlarmRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AlarmViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AlarmViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}