package com.jin.subwayalarm.ui.component

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.jin.subwayalarm.ui.theme.Hls

/** HONEYLOVESOFT 의 pill 모양(radius 980). */
val PillShape = RoundedCornerShape(percent = 50)

private val NUMBERED_LINE = Regex("""^(\d+)호선$""")
private val HOSEON_SUFFIX = Regex("""^(.+?)호선$""")

/**
 * 노선 배지 글자. "3호선" → "3"(원형 배지), "인천1호선" → "인천1", "신분당선" → "신분당", "김포골드라인" → "김포골드".
 * 노선 공식색은 쓰지 않는다 — 디자인 시스템이 색을 [Hls.Accent] 하나로 제한한다.
 */
fun lineShortLabel(lineName: String): String {
    HOSEON_SUFFIX.find(lineName)?.let { return it.groupValues[1] } // "3호선"→"3", "인천1호선"→"인천1"
    return lineName.removeSuffix("라인").removeSuffix("선").ifBlank { lineName }
}

/** "3호선","8호선" → "3·8호선". 번호 노선이 아닌 것은 뒤에 그대로 붙인다. */
fun compactLineNames(lineNames: List<String>): String {
    val numbered = lineNames.mapNotNull { NUMBERED_LINE.find(it)?.groupValues?.get(1) }
    val others = lineNames.filter { NUMBERED_LINE.find(it) == null }
    val parts = buildList {
        if (numbered.isNotEmpty()) add(numbered.joinToString("·") + "호선")
        addAll(others)
    }
    return parts.joinToString("·")
}

/** 노선 배지 — 번호 노선은 원형, 그 외는 둥근 사각. [inverted] = 파란 바탕 위(흰 배지·파란 글자). */
@Composable
fun LineBadge(lineName: String, size: Dp = 22.dp, inverted: Boolean = false) {
    val label = lineShortLabel(lineName)
    val bg = if (inverted) Hls.Paper else Hls.Accent
    val fg = if (inverted) Hls.AccentDeep else Hls.OnAccent
    val isNumber = label.all { it.isDigit() }
    val fontSize = (size.value * if (isNumber) 0.55f else 0.5f).sp
    Box(
        modifier = Modifier
            .then(if (isNumber) Modifier.size(size).clip(CircleShape) else Modifier.height(size).clip(RoundedCornerShape(6.dp)))
            .background(bg)
            .then(if (isNumber) Modifier else Modifier.padding(horizontal = 6.dp))
            .semantics { contentDescription = lineName },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = fg, fontSize = fontSize, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

/** 파란 채움 pill — 화면의 주 동작 하나에만. */
@Composable
fun PrimaryPill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 52.dp,
    fontSize: TextUnit = 17.sp,
    container: Color = Hls.Accent,
    content: Color = Hls.OnAccent,
) {
    Box(
        modifier = modifier
            .height(height)
            .clip(PillShape)
            .background(if (enabled) container else Hls.LineSoft)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = if (enabled) content else Hls.Faint,
            fontSize = fontSize,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}

/** 1px 테두리 pill — 보조 동작. */
@Composable
fun OutlinePill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 44.dp,
    fontSize: TextUnit = 14.sp,
    textColor: Color = Hls.Ink,
    borderColor: Color = Hls.Line,
    container: Color = Hls.Paper,
    contentPadding: PaddingValues = PaddingValues(horizontal = 14.dp),
) {
    Box(
        modifier = modifier
            .height(height)
            .clip(PillShape)
            .background(container)
            .border(1.dp, borderColor, PillShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = if (enabled) textColor else Hls.Faint,
            fontSize = fontSize,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}

/** 글자 링크 — 최소 44dp 터치 영역. */
@Composable
fun TextLink(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Hls.InkSoft,
    fontSize: TextUnit = 14.sp,
    fontWeight: FontWeight = FontWeight.Normal,
) {
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 44.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = color, fontSize = fontSize, fontWeight = fontWeight, maxLines = 1)
    }
}

/** 섹션 머리 작은 글씨(파란 진한색). */
@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier, color: Color = Hls.AccentDeep) {
    Text(text, modifier = modifier, color = color, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
}

/** 켜짐/꺼짐 스위치 (52×32, 켜짐 = 파랑). */
@Composable
fun HlsSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, label: String) {
    Row(
        modifier = Modifier
            .width(52.dp)
            .height(32.dp)
            .clip(PillShape)
            .background(if (checked) Hls.Accent else Hls.Faint)
            .clickable(role = Role.Switch) { onCheckedChange(!checked) }
            .semantics {
                contentDescription = label
                stateDescription = if (checked) "켜짐" else "꺼짐"
            }
            .padding(2.dp),
        horizontalArrangement = if (checked) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(Hls.Paper))
    }
}

/**
 * 상태바 아이콘 색. [lightIcons] = 어두운(잉크·파랑) 바탕 위 흰 아이콘.
 *
 * 화면마다 부르면 전환 애니메이션 동안 새 화면이 먼저 설정하고 옛 화면의 정리가 나중에 덮어써서
 * 알람·추적 화면 위에 검은 아이콘이 남았다. 그래서 내비게이션 수준에서 현재 목적지로 한 번만 정한다.
 */
@Composable
fun StatusBarIcons(lightIcons: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !lightIcons
    }
}
