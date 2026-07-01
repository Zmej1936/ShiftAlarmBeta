package com.example.shiftalarm.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AlarmRepository {
    private var storage: AlarmStorage? = null

    // Реактивный модуль, за которым будет следить наш UI-экран
    private val _alarmsFlow = MutableStateFlow<List<Alarm>>(emptyList())
    val alarmsFlow: StateFlow<List<Alarm>> = _alarmsFlow.asStateFlow()

    // Инициализация при старте приложения
    fun initialize(context: Context) {
        if (storage == null) {
            storage = AlarmStorage(context.applicationContext)
            _alarmsFlow.value = storage?.getAlarms() ?: emptyList()
        }
    }

    // Сохранение нового списка
    fun saveAlarms(context: Context, alarms: List<Alarm>) {
        initialize(context)
        storage?.saveAlarms(alarms)
        _alarmsFlow.value = alarms
    }

    // Обновление одного будильника (вызывается и на экранах, и в ресиверах!)
    fun updateAlarm(context: Context, updatedAlarm: Alarm) {
        initialize(context)
        storage?.updateAlarm(updatedAlarm)
        // Магия: принудительно обновляем поток, и Compose мгновенно перерисовывает дату!
        _alarmsFlow.value = storage?.getAlarms() ?: emptyList()
    }
}