package com.example.shiftalarm.viewmodel

import android.app.Application
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val android.content.Context.dataStore by preferencesDataStore("settings")

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    private val START_DATE_KEY = stringPreferencesKey("start_date")

    private val _startDate = MutableStateFlow("2026-05-19")
    val startDate: StateFlow<String> = _startDate

    init {
        viewModelScope.launch {
            context.dataStore.data.map { preferences ->
                preferences[START_DATE_KEY] ?: "2026-05-19"
            }.collect { date ->
                _startDate.value = date
                // ИСПРАВЛЕНО: Убран вызов удаленного метода ShiftCalculator.setStartDate(date)
            }
        }
    }

    // ИСПРАВЛЕНО: Убран лишний suspend (так как внутри запускается корутина) и добавлен context.
    fun saveStartDate(date: String) {
        viewModelScope.launch {
            context.dataStore.edit { preferences ->
                preferences[START_DATE_KEY] = date
            }
        }
    }
}