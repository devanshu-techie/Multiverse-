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
    primary = Color(0xFF9D7BFF),
    onPrimary = Color.White,
    secondary = MvMint,
    onSecondary = MvDark,
    tertiary = MvSky,
    error = MvCoral,
    background = Color(0xFF0E0B1E),          // deep space
    onBackground = Color(0xFFF1EFFA),
    surface = Color(0xFF1D1832),
    onSurface = Color(0xFFF1EFFA),
    surfaceVariant = Color(0xFF2E2A45),
    onSurfaceVariant = Color(0xFFB9B3D6),
)

/** Futuristic neon look: the app is dark-first (pass darkTheme = false to opt out). */
@Composable
fun MiniVerseTheme(darkTheme: Boolean = true, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}

/** Seat colours for up to 4 local players. */
val SeatColors = listOf(MvPurple, MvCoral, MvSky, MvMint)

// Extra candy colours for a kid-friendly look
val MvOrange = Color(0xFFFFA14D)
val MvPink = Color(0xFFFF8AC2)
val MvLemon = Color(0xFFFFF3B0)

/** Soft pastel backgrounds cycled across game cards. */
val CardPastels = listOf(
    Color(0xFFEFEAFD), // lavender
    Color(0xFFFFE9E9), // blush
    Color(0xFFE3F5FF), // baby blue
    Color(0xFFE4FBF1), // mint cream
    Color(0xFFFFF3DC), // peach
    Color(0xFFFDE8FF), // lilac pink
)

/** Bold accent per category chip. */
fun categoryColor(category: String): Color = when (category) {
    "Board" -> MvSky
    "Party" -> MvCoral
    "Puzzle" -> MvMint
    "Arcade" -> MvOrange
    else -> MvPurple
}
