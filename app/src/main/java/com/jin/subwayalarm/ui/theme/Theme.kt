package com.jin.subwayalarm.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * "다왔다" 앱 테마 — HONEYLOVESOFT 디자인 시스템. 밝은 화면 고정, 추적 화면 상단만 [Hls.Ink] 띠.
 */
private val LightColors = lightColorScheme(
    primary = Hls.Accent,
    onPrimary = Hls.OnAccent,
    secondary = Hls.AccentDeep,
    background = Hls.Paper,
    onBackground = Hls.Ink,
    surface = Hls.Card,
    onSurface = Hls.Ink,
    surfaceVariant = Hls.CardAlt,
    onSurfaceVariant = Hls.InkSoft,
    error = Hls.ErrorText,
    outline = Hls.Line,
    outlineVariant = Hls.LineSoft,
)

@Composable
fun SubwayAlarmTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content,
    )
}
