package com.devanshu.miniverse

import android.app.Application
import com.devanshu.miniverse.audio.Sfx
import com.devanshu.miniverse.audio.Voice
import com.devanshu.miniverse.data.AppDatabase
import com.devanshu.miniverse.data.GameRepository
import com.devanshu.miniverse.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Manual DI container (kept simple; Hilt is a later refactor). */
class MiniVerseApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.get(this) }
    val repository: GameRepository by lazy { GameRepository(database) }
    val settings: SettingsRepository by lazy { SettingsRepository(this) }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        Sfx.init(this)
        Voice.init(this)
        // Keep the sound switch in sync with Settings.
        appScope.launch { settings.settings.collect { Sfx.enabled = it.sound } }
    }
}
