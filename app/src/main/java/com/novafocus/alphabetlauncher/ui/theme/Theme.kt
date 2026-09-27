package com.novafocus.alphabetlauncher.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    background = Color.Black,
    surface = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
    primary = Color(0xFF9FC7FF),
    onPrimary = Color.Black,
)

private val LightColors = lightColorScheme(
    background = Color(0xFFF2F3F7),
    surface = Color(0xFFF2F3F7),
    onBackground = Color.Black,
    onSurface = Color.Black,
)

// Defaults to the system theme, but darkTheme can be overridden by the
// sun/moon toggle button on the home screen.
@Composable
fun AlphabetLauncherTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors, content = content)
}
