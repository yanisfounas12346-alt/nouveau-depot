package com.yanis.objectif30.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "objectif30_preferences")

data class UserSettings(
    val currentWeight: Double = 90.0,
    val targetWeight: Double = 80.0,
    val reminderEnabled: Boolean = true,
    val reminderHour: Int = 19,
    val reminderMinute: Int = 0
)

class UserPreferences(private val context: Context) {
    private object Keys {
        val currentWeight = doublePreferencesKey("current_weight")
        val reminderEnabled = booleanPreferencesKey("reminder_enabled")
        val reminderHour = intPreferencesKey("reminder_hour")
        val reminderMinute = intPreferencesKey("reminder_minute")
    }

    val settings: Flow<UserSettings> = context.dataStore.data.map { p ->
        UserSettings(
            currentWeight = p[Keys.currentWeight] ?: 90.0,
            reminderEnabled = p[Keys.reminderEnabled] ?: true,
            reminderHour = p[Keys.reminderHour] ?: 19,
            reminderMinute = p[Keys.reminderMinute] ?: 0
        )
    }

    suspend fun setWeight(value: Double) {
        context.dataStore.edit { it[Keys.currentWeight] = value }
    }

    suspend fun setReminder(enabled: Boolean, hour: Int, minute: Int) {
        context.dataStore.edit {
            it[Keys.reminderEnabled] = enabled
            it[Keys.reminderHour] = hour
            it[Keys.reminderMinute] = minute
        }
    }
}
