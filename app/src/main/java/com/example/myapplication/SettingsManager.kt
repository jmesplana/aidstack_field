package com.example.myapplication

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

enum class UnitSystem {
    METRIC, IMPERIAL
}

class SettingsManager(private val context: Context) {
    companion object {
        private val USE_METRIC = booleanPreferencesKey("use_metric")
        private val UPDATE_INTERVAL = intPreferencesKey("update_interval")
    }

    val useMetric: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[USE_METRIC] ?: true
    }

    val updateInterval: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[UPDATE_INTERVAL] ?: 5000
    }

    suspend fun setUseMetric(useMetric: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[USE_METRIC] = useMetric
        }
    }

    suspend fun setUpdateInterval(interval: Int) {
        context.dataStore.edit { preferences ->
            preferences[UPDATE_INTERVAL] = interval
        }
    }
}
