package com.example.shiftalarm

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore("settings")

val Context.alarmsDataStore: DataStore<Preferences> by preferencesDataStore("alarms_prefs")