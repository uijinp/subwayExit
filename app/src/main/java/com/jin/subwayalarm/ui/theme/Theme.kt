package com.jin.subwayalarm.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SubwayBlue = Color(0xFF1F6FEB)
private val SubwayBlueDark = Color(0xFF1457C2)

private val LightColors = lightColorScheme(
    primary = SubwayBlue,
    secondary = SubwayBlueDark,
)

private val DarkColors = darkColorScheme(
    primary = SubwayBlue,
    secondary = SubwayBlueDark,
)

@Composable
fun SubwayAlarmTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
