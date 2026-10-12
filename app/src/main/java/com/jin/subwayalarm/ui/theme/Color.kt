package com.jin.subwayalarm.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * HONEYLOVESOFT 디자인 시스템 토큰 (tokens.json 의 color 값 그대로).
 *
 * 흑백 + 시스템 블루 하나. 깊이는 그림자가 아니라 1px [Line] 테두리와 바탕색 변화로 낸다.
 * - 텍스트(밝은 바탕): [Ink] → [InkSoft] → [Muted]. 작은 새 텍스트는 [Muted] 까지만.
 * - 텍스트(어두운 바탕 [Ink]): [OnDark] → [OnDarkSub] → [OnDarkSub2].
 * - [Accent] 는 유일한 색. 채움 위 글자는 항상 흰색. 밝은 바탕의 작은 강조 글자는 [AccentDeep],
 *   어두운 바탕에선 [AccentLight].
 * - 강조 패널: [AccentTint] 채움 + [AccentSoft] 테두리([AccentSoft] 는 글자에 쓰지 않음).
 * - [ErrorText] 는 오류 안내에만.
 */
object Hls {
    val Paper = Color(0xFFFFFFFF)
    val Card = Color(0xFFFFFFFF)
    val CardAlt = Color(0xFFF5F5F7)
    val Ink = Color(0xFF1D1D1F)
    val InkSoft = Color(0xFF424245)
    val Muted = Color(0xFF6E6E73)
    val Faint = Color(0xFF86868B)
    val Line = Color(0xFFD2D2D7)
    val LineSoft = Color(0xFFE8E8ED)

    val Accent = Color(0xFF0071E3)
    val AccentDeep = Color(0xFF0058B0)
    val AccentLight = Color(0xFF2997FF)
    val AccentSoft = Color(0xFFA6D2FF)
    val AccentTint = Color(0xFFF0F7FF)
    val OnAccent = Color(0xFFFFFFFF)

    val OnDark = Color(0xFFF5F5F7)
    val OnDarkSub = Color(0xFFA1A1A6)
    val OnDarkSub2 = Color(0xFF86868B)
    val DarkLine = Color(0xFF2A2A2C)

    val ErrorText = Color(0xFFD6293E)
}
