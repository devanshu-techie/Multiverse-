package com.devanshu.miniverse.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
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

    // ---- Saved friends (names typed once, reused in every game) ----
    private val friendsKey = stringPreferencesKey("friends")

    val friends: Flow<List<String>> = context.settingsStore.data.map { p ->
        (p[friendsKey] ?: "").split("\n").filter { it.isNotBlank() }
    }

    suspend fun addFriend(name: String) {
        val clean = name.trim().replace("\n", " ")
        if (clean.length < 2) return
        context.settingsStore.edit { p ->
            val list = (p[friendsKey] ?: "").split("\n").filter { it.isNotBlank() }
            if (list.none { it.equals(clean, ignoreCase = true) }) {
                p[friendsKey] = (list + clean).joinToString("\n")
            }
        }
    }

    suspend fun removeFriend(name: String) {
        context.settingsStore.edit { p ->
            val list = (p[friendsKey] ?: "").split("\n").filter { it.isNotBlank() && it != name }
            p[friendsKey] = list.joinToString("\n")
        }
    }

    // ---- Saved match (continue later or restart - the player decides) ----
    private val savedMatchKey = stringPreferencesKey("saved_match")

    val savedMatch: Flow<SavedMatch?> = context.settingsStore.data.map { p ->
        val raw = p[savedMatchKey] ?: return@map null
        val parts = raw.split("\n", limit = 3)
        if (parts.size == 3) SavedMatch(parts[0], parts[1], parts[2]) else null
    }

    suspend fun saveMatch(match: SavedMatch) = context.settingsStore.edit {
        it[savedMatchKey] = match.matchId + "\n" + match.gameId + "\n" + match.stateJson
    }

    suspend fun clearSavedMatch() = context.settingsStore.edit { it.remove(savedMatchKey) }
}

/** An unfinished match the player can continue. stateJson comes from MiniGame.encodeState. */
data class SavedMatch(val matchId: String, val gameId: String, val stateJson: String)
