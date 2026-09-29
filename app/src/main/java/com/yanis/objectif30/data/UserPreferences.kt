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
    val reminderMinute: Int = 0,
    val trainingHour: Int = 18,
    val trainingMinute: Int = 30,
    val breakfastChoice: Int = 0,
    val lunchChoice: Int = 0,
    val snackChoice: Int = 0,
    val dinnerChoice: Int = 0,
    val pullUps: Int = 0,
    val pushUps: Int = 0,
    val easyRunMinutes: Int = 25,
    val shadowRounds: Int = 4,
    val energyScore: Int = 7,
    val hungerScore: Int = 4
)

class UserPreferences(private val context: Context) {
    private object Keys {
        val currentWeight = doublePreferencesKey("current_weight")
        val reminderEnabled = booleanPreferencesKey("reminder_enabled")
        val reminderHour = intPreferencesKey("reminder_hour")
        val reminderMinute = intPreferencesKey("reminder_minute")
        val trainingHour = intPreferencesKey("training_hour")
        val trainingMinute = intPreferencesKey("training_minute")
        val breakfastChoice = intPreferencesKey("breakfast_choice")
        val lunchChoice = intPreferencesKey("lunch_choice")
        val snackChoice = intPreferencesKey("snack_choice")
        val dinnerChoice = intPreferencesKey("dinner_choice")
        val pullUps = intPreferencesKey("pull_ups")
        val pushUps = intPreferencesKey("push_ups")
        val easyRunMinutes = intPreferencesKey("easy_run_minutes")
        val shadowRounds = intPreferencesKey("shadow_rounds")
        val energyScore = intPreferencesKey("energy_score")
        val hungerScore = intPreferencesKey("hunger_score")
    }

    val settings: Flow<UserSettings> = context.dataStore.data.map { p ->
        UserSettings(
            currentWeight = p[Keys.currentWeight] ?: 90.0,
            reminderEnabled = p[Keys.reminderEnabled] ?: true,
            reminderHour = p[Keys.reminderHour] ?: 19,
            reminderMinute = p[Keys.reminderMinute] ?: 0,
            trainingHour = p[Keys.trainingHour] ?: 18,
            trainingMinute = p[Keys.trainingMinute] ?: 30,
            breakfastChoice = p[Keys.breakfastChoice] ?: 0,
            lunchChoice = p[Keys.lunchChoice] ?: 0,
            snackChoice = p[Keys.snackChoice] ?: 0,
            dinnerChoice = p[Keys.dinnerChoice] ?: 0,
            pullUps = p[Keys.pullUps] ?: 0,
            pushUps = p[Keys.pushUps] ?: 0,
            easyRunMinutes = p[Keys.easyRunMinutes] ?: 25,
            shadowRounds = p[Keys.shadowRounds] ?: 4,
            energyScore = p[Keys.energyScore] ?: 7,
            hungerScore = p[Keys.hungerScore] ?: 4
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

    suspend fun setTrainingTime(hour: Int, minute: Int) {
        context.dataStore.edit {
            it[Keys.trainingHour] = hour
            it[Keys.trainingMinute] = minute
        }
    }

    suspend fun setMealChoice(slot: MealSlot, index: Int) {
        context.dataStore.edit {
            when (slot) {
                MealSlot.BREAKFAST -> it[Keys.breakfastChoice] = index
                MealSlot.LUNCH -> it[Keys.lunchChoice] = index
                MealSlot.SNACK -> it[Keys.snackChoice] = index
                MealSlot.DINNER -> it[Keys.dinnerChoice] = index
            }
        }
    }

    suspend fun setPerformance(
        pullUps: Int,
        pushUps: Int,
        easyRunMinutes: Int,
        shadowRounds: Int,
        energyScore: Int,
        hungerScore: Int
    ) {
        context.dataStore.edit {
            it[Keys.pullUps] = pullUps
            it[Keys.pushUps] = pushUps
            it[Keys.easyRunMinutes] = easyRunMinutes
            it[Keys.shadowRounds] = shadowRounds
            it[Keys.energyScore] = energyScore.coerceIn(1, 10)
            it[Keys.hungerScore] = hungerScore.coerceIn(1, 10)
        }
    }
}
