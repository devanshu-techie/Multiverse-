package com.devanshu.miniverse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.devanshu.miniverse.ui.MiniVerseNavHost
import com.devanshu.miniverse.ui.theme.MiniVerseTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as MiniVerseApp
        setContent {
            MiniVerseTheme {
                MiniVerseNavHost(repository = app.repository)
            }
        }
    }
}
