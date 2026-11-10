package com.devanshu.miniverse

import android.app.Application
import com.devanshu.miniverse.data.AppDatabase
import com.devanshu.miniverse.data.GameRepository

/** Manual DI container (kept simple for v0.1; Hilt is a later refactor). */
class MiniVerseApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.get(this) }
    val repository: GameRepository by lazy { GameRepository(database) }
}
