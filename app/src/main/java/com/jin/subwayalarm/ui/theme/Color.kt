package com.jin.subwayalarm.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * "다왔다" 디자인 토큰. Claude Design 목업에서 추출한 실측 색상값.
 * 브라우즈 화면(홈·검색·알람설정)은 라이트, 추적 화면은 다크 테마.
 */
object DawatColors {
    // 포인트(지하철 그린)
    val Green = Color(0xFF33D17F)
    val GreenDeep = Color(0xFF00C766)
    val GreenTint = Color(0xFFEEF6F0)
    val OnGreen = Color(0xFF08150D)

    // 알람(코랄)
    val Coral = Color(0xFFFF5A45)
    val CoralEnd = Color(0xFFFF6A52)
    val CoralTint = Color(0xFFFDEEEB)

    // 라이트 테마
    val LightBg = Color(0xFFF1F3F5)
    val Card = Color(0xFFFFFFFF)
    val HeroCard = Color(0xFF16191C)
    val TextPrimary = Color(0xFF0B0D0F)
    val TextMuted = Color(0xFF5F6B73)
    val Divider = Color(0xFFECEEF0)

    // 다크 테마(추적)
    val DarkBg = Color(0xFF0B0D0F)
    val DarkCard = Color(0xFF1A1E22)
    val OnDark = Color(0xFFFFFFFF)
    val OnDarkMuted = Color(0xFFAAB3BA)
    val OnDarkDim = Color(0xFF5F6B73)
}
