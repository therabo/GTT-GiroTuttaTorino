package it.girotuttatorino.gtt.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = GttBlue,
    secondary = GttOrange,
)

private val DarkColorScheme = darkColorScheme(
    primary = GttLightBlue,
    secondary = GttOrange,
    background = GttDarkBlue,
)

@Composable
fun GTTTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        content = content,
    )
}
