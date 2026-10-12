package com.jin.subwayalarm.ui.screen.alarm

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jin.subwayalarm.domain.Josa
import com.jin.subwayalarm.ui.component.LineBadge
import com.jin.subwayalarm.ui.component.PrimaryPill
import com.jin.subwayalarm.ui.theme.Hls

/**
 * 전체화면 알람(잠금화면 위에도 뜸). 파란 바탕에 "약수에서 환승하세요" 를 크게.
 * 소리·진동은 서비스가 울리고 있으므로 이 화면은 "확인했어요" 로 끄는 것만 한다. 뒤로가기로는 닫히지 않는다.
 */
@Composable
fun AlarmScreen(
    /** isTransfer: 다음 구간이 있어 추적 화면으로 이어가야 하는지, 없으면(마지막) 홈으로. */
    onDismissed: (isTransfer: Boolean) -> Unit,
    viewModel: AlarmViewModel = hiltViewModel(),
) {
    val loadedState by viewModel.uiState.collectAsStateWithLifecycle()
    // 확인 버튼과 "이미 꺼짐" 감지가 동시에 화면을 넘기지 않게(퇴장 애니메이션 중에도 이 화면은 잠시 살아 있다).
    var leaving by remember { mutableStateOf(false) }
    BackHandler { /* 알람은 "확인했어요" 로만 끈다 */ }

    LaunchedEffect(loadedState) {
        val state = loadedState
        if (!leaving && state is AlarmViewModel.UiState.NotRinging) {
            leaving = true
            onDismissed(state.tripContinues)
        }
    }

    val ringing = loadedState as? AlarmViewModel.UiState.Ringing

    Box(modifier = Modifier.fillMaxSize().background(Hls.Accent)) {
        RingDecoration()
        if (ringing != null) {
            AlarmContent(
                state = ringing,
                onConfirm = {
                    if (!leaving) {
                        leaving = true
                        viewModel.dismiss()
                        onDismissed(ringing.isTransfer)
                    }
                },
            )
        }
    }
}

@Composable
private fun AlarmContent(state: AlarmViewModel.UiState.Ringing, onConfirm: () -> Unit) {
    val nextLine = state.nextLeg?.lineName.orEmpty()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(start = 26.dp, end = 26.dp, top = 24.dp, bottom = 28.dp),
    ) {
        Text(
            if (state.isTransfer) "다왔다 · 환승 알람" else "다왔다 · 하차 알람",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Hls.OnAccent,
        )

        Column(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterVertically),
        ) {
            if (state.passedStationName.isNotEmpty() && state.passedStationName != state.stationName) {
                Text(
                    "${state.passedStationName} 통과 · ${state.whenLabel}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Hls.OnAccent,
                )
            }
            Text(
                if (state.isTransfer) "${state.stationName}에서\n환승하세요" else "${state.stationName}에서\n내리세요",
                style = TextStyle(
                    fontSize = 52.sp,
                    lineHeight = 56.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Hls.OnAccent,
                ),
            )
            Text(
                if (state.isTransfer) {
                    val leg = state.nextLeg
                    if (leg != null && leg.boardStationName != state.stationName) {
                        "내려서 ${leg.boardStationName}까지 걸어가 ${Josa.ro(nextLine)} 갈아타세요"
                    } else {
                        "내려서 ${Josa.ro(nextLine)} 갈아타세요"
                    }
                } else {
                    "두고 내리는 물건이 없는지 확인하세요"
                },
                fontSize = 19.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.SemiBold,
                color = Hls.OnAccent,
            )
            state.nextLeg?.let { leg ->
                NextLegCard(
                    lineName = leg.lineName,
                    title = buildString {
                        append("${leg.boardStationName} → ${leg.alightStationName}")
                        state.nextLegStops?.let { append(" · ${it}정거장") }
                    },
                    caption = "다음 구간 · ${state.nextLegNumber}/${state.totalLegs}",
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                repeatNote(state),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Hls.OnAccent,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            PrimaryPill(
                "확인했어요",
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(),
                height = 64.dp,
                fontSize = 19.sp,
                container = Hls.Paper,
                content = Hls.Ink,
            )
            Text(
                if (state.isTransfer) "확인하면 $nextLine 추적을 바로 시작합니다" else "확인하면 여정을 마칩니다",
                fontSize = 13.sp,
                color = Hls.OnAccent,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private fun repeatNote(state: AlarmViewModel.UiState.Ringing): String {
    val what = if (state.setting.soundEnabled) "진동과 소리가" else "진동이"
    return if (state.setting.repeatUntilDismissed) "↻ 확인할 때까지 $what 이어집니다" else "알람은 한 번만 울립니다"
}

@Composable
private fun NextLegCard(lineName: String, title: String, caption: String) {
    Row(
        modifier = Modifier
            .padding(top = 8.dp)
            .fillMaxWidth()
            .border(1.dp, Hls.AccentSoft, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LineBadge(lineName, size = 32.dp, inverted = true)
        Spacer(modifier = Modifier.width(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(caption, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Hls.OnAccent)
            Text(title, fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Bold, color = Hls.OnAccent, maxLines = 2)
        }
    }
}

/** 오른쪽 위 동심원 두 개(실선·점선) — 울리고 있다는 느낌만, 정보는 없다. */
@Composable
private fun RingDecoration() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width - 50.dp.toPx(), 90.dp.toPx())
        drawCircle(
            color = Color.White.copy(alpha = 0.18f),
            radius = 210.dp.toPx(),
            center = center,
            style = Stroke(width = 1.dp.toPx()),
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.22f),
            radius = 140.dp.toPx(),
            center = center,
            style = Stroke(
                width = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx())),
            ),
        )
    }
}
