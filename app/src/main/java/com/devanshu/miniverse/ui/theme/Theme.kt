package com.devanshu.miniverse.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// MiniVerse palette (build explainer section 2)
val MvPurple = Color(0xFF7955E8)
val MvYellow = Color(0xFFFFD84D)
val MvCoral = Color(0xFFFF6B6B)
val MvSky = Color(0xFF4DB9F5)
val MvMint = Color(0xFF55D6A5)
val MvDark = Color(0xFF171523)
val MvCream = Color(0xFFFFF9F0)

private val LightColors = lightColorScheme(
    primary = MvPurple,
    onPrimary = Color.White,
    secondary = MvMint,
    onSecondary = MvDark,
    tertiary = MvSky,
    error = MvCoral,
    background = MvCream,
    onBackground = MvDark,
    surface = Color.White,
    onSurface = MvDark,
    surfaceVariant = Color(0xFFEFEAFD),
    onSurfaceVariant = Color(0xFF5B5870),
)

private val DarkColors = darkColorScheme(
    primary = MvPurple,
    onPrimary = Color.White,
    secondary = MvMint,
    onSecondary = MvDark,
    tertiary = MvSky,
    error = MvCoral,
    background = MvDark,
    onBackground = Color(0xFFF1EFFA),
    surface = Color(0xFF221F33),
    onSurface = Color(0xFFF1EFFA),
    surfaceVariant = Color(0xFF2E2A45),
    onSurfaceVariant = Color(0xFFB9B3D6),
)

@Composable
fun MiniVerseTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}

/** Seat colours for up to 4 local players. */
val SeatColors = listOf(MvPurple, MvCoral, MvSky, MvMint)
