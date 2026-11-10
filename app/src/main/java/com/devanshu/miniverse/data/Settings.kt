package com.devanshu.miniverse.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsStore by preferencesDataStore(name = "settings")

data class AppSettings(
    val sound: Boolean = true,
    val vibration: Boolean = true,
    val reducedMotion: Boolean = false,
)

class SettingsRepository(private val context: Context) {
    private val soundKey = booleanPreferencesKey("sound")
    private val vibrationKey = booleanPreferencesKey("vibration")
    private val reducedMotionKey = booleanPreferencesKey("reduced_motion")

    val settings: Flow<AppSettings> = context.settingsStore.data.map { p ->
        AppSettings(
            sound = p[soundKey] ?: true,
            vibration = p[vibrationKey] ?: true,
            reducedMotion = p[reducedMotionKey] ?: false,
        )
    }

    suspend fun setSound(on: Boolean) = context.settingsStore.edit { it[soundKey] = on }
    suspend fun setVibration(on: Boolean) = context.settingsStore.edit { it[vibrationKey] = on }
    suspend fun setReducedMotion(on: Boolean) = context.settingsStore.edit { it[reducedMotionKey] = on }
}
