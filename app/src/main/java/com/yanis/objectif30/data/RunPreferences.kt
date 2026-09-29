package com.yanis.objectif30.data

import android.content.Context
import android.content.SharedPreferences

class RunPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("wildsport_run", Context.MODE_PRIVATE)

    fun targetSpeedKmh(): Float = prefs.getFloat("target_speed_kmh", 0f)
    fun targetMinutes(): Int = prefs.getInt("target_minutes", 30)

    fun saveTargetSpeedKmh(value: Float) {
        prefs.edit().putFloat("target_speed_kmh", value.coerceIn(4f, 20f)).apply()
    }

    fun resetCalibration() {
        prefs.edit().remove("target_speed_kmh").apply()
    }

    fun saveTargetMinutes(value: Int) {
        prefs.edit().putInt("target_minutes", value.coerceIn(15, 90)).apply()
    }

    fun saveLastRun(distanceKm: Float, avgKmh: Float, steps: Int, seconds: Int) {
        prefs.edit()
            .putFloat("last_distance_km", distanceKm.coerceAtLeast(0f))
            .putFloat("last_avg_kmh", avgKmh.coerceAtLeast(0f))
            .putInt("last_steps", steps.coerceAtLeast(0))
            .putInt("last_seconds", seconds.coerceAtLeast(0))
            .apply()
    }

    fun lastDistanceKm(): Float = prefs.getFloat("last_distance_km", 0f)
    fun lastAvgKmh(): Float = prefs.getFloat("last_avg_kmh", 0f)
    fun lastSteps(): Int = prefs.getInt("last_steps", 0)
    fun lastSeconds(): Int = prefs.getInt("last_seconds", 0)
}
