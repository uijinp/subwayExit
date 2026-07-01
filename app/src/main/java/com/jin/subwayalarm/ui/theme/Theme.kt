package com.jin.subwayalarm.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * "다왔다" 앱 테마. 브라우즈 화면은 라이트 고정(디자인 기준). 추적 화면은 자체 다크 surface 사용.
 */
private val LightColors = lightColorScheme(
    primary = DawatColors.Green,
    onPrimary = DawatColors.OnGreen,
    secondary = DawatColors.GreenDeep,
    background = DawatColors.LightBg,
    onBackground = DawatColors.TextPrimary,
    surface = DawatColors.Card,
    onSurface = DawatColors.TextPrimary,
    surfaceVariant = DawatColors.GreenTint,
    error = DawatColors.Coral,
    outline = DawatColors.TextMuted,
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
