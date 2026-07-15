package com.jin.subwayalarm.ui.screen.tracking

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jin.subwayalarm.domain.model.BoardingCandidate
import com.jin.subwayalarm.domain.model.TrackingMode
import com.jin.subwayalarm.domain.model.TripState
import com.jin.subwayalarm.tracking.TrackingService
import com.jin.subwayalarm.ui.theme.DawatColors

@Composable
fun TrackingScreen(
    onSessionEnded: () -> Unit,
    viewModel: TrackingViewModel = hiltViewModel(),
) {
    val loadedState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(loadedState) {
        val state = loadedState
        if (state != null && !state.isActive) onSessionEnded()
    }

    val uiState = loadedState ?: return

    Scaffold(containerColor = DawatColors.DarkBg) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).background(DawatColors.DarkBg)) {
            Header(uiState)

            if (uiState.state == TripState.ALERTED) {
                AlarmBanner(
                    alertStationName = uiState.currentStationName,
                    destinationName = uiState.destinationName,
                    isTransfer = uiState.isTransferLeg,
                    nextLineName = uiState.nextLineName,
                    onAcknowledge = viewModel::acknowledgeAlarm,
                )
            }

            if (uiState.state != TripState.ALERTED && uiState.candidates.isNotEmpty()) {
                CandidatePanel(
                    title = if (uiState.state == TripState.MATCHING) "탑승 열차를 선택하세요" else "다른 열차로 바꾸기",
                    candidates = uiState.candidates,
                    boardedTrainNo = uiState.boardedTrainNo,
                    onSelect = { trainNo ->
                        ContextCompat.startForegroundService(
                            context,
                            TrackingService.confirmTrainIntent(context, trainNo),
                        )
                    },
                )
            }

            if (uiState.showRematch && uiState.state == TripState.TRACKING) {
                OutlinedButton(
                    onClick = {
                        ContextCompat.startForegroundService(context, TrackingService.rematchIntent(context))
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 4.dp),
                ) {
                    Text("다른 열차?", color = DawatColors.OnDark)
                }
            }

            if (uiState.state == TripState.MATCHING && uiState.candidates.isEmpty()) {
                Text(
                    "출발역에서 열차를 찾는 중…",
                    color = DawatColors.OnDarkMuted,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )
            }

            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 24.dp)) {
                items(uiState.route, key = { it.statnId }) { station ->
                    RouteStationRow(station)
                }
            }

            Button(
                onClick = { context.startService(TrackingService.stopIntent(context)) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = DawatColors.DarkCard,
                    contentColor = DawatColors.OnDarkMuted,
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(24.dp).height(52.dp),
            ) {
                Text("추적 정지")
            }
        }
    }
}

@Composable
private fun CandidatePanel(
    title: String,
    candidates: List<BoardingCandidate>,
    boardedTrainNo: String?,
    onSelect: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(DawatColors.DarkCard)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, color = DawatColors.OnDark, fontWeight = FontWeight.Bold)
        candidates.forEach { c ->
            val label = buildString {
                append("#${c.trainNo}")
                append(" · ${c.statnName}")
                c.terminusName?.let { append(" → $it") }
                if (c.isExpress) append(" · 급행")
                if (c.trainNo == boardedTrainNo) append(" (현재)")
            }
            Button(
                onClick = { onSelect(c.trainNo) },
                enabled = c.trainNo != boardedTrainNo,
                colors = ButtonDefaults.buttonColors(
                    containerColor = DawatColors.Green,
                    contentColor = DawatColors.OnGreen,
                    disabledContainerColor = DawatColors.OnDarkDim,
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(label, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun Header(uiState: TrackingViewModel.UiState) {
    Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(DawatColors.Green))
                    Spacer(modifier = Modifier.width(6.dp))
                    val statusLabel = when {
                        uiState.state == TripState.MATCHING -> "탑승 확인 중"
                        uiState.mode == TrackingMode.ACCEL -> "추정 추적 중"
                        else -> "추적 중"
                    }
                    val legLabel = if (uiState.totalLegs > 1) " · ${uiState.currentLegNumber}/${uiState.totalLegs}구간" else ""
                    Text(
                        "$statusLabel · ${uiState.lineName}$legLabel",
                        style = MaterialTheme.typography.labelMedium,
                        color = DawatColors.OnDarkMuted,
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "${uiState.originName} → ${uiState.destinationName}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = DawatColors.OnDark,
                )
                uiState.boardedTrainNo?.let {
                    Text("열차 $it", color = DawatColors.OnDarkMuted, style = MaterialTheme.typography.labelMedium)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${uiState.remainingStops}",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    color = DawatColors.OnDark,
                )
                Text("정거장 남음", style = MaterialTheme.typography.labelMedium, color = DawatColors.OnDarkMuted)
            }
        }
    }
}

@Composable
private fun AlarmBanner(
    alertStationName: String,
    destinationName: String,
    isTransfer: Boolean,
    nextLineName: String,
    onAcknowledge: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(DawatColors.Coral)
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) { Text(if (isTransfer) "🔄" else "🔔", fontSize = 18.sp) }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                val headline = if (isTransfer) "환승하세요!" else "이제 곧 내리세요!"
                val detail = if (isTransfer) {
                    "$alertStationName 통과 · 여기서 내려 ${nextLineName}으로 갈아타세요"
                } else {
                    "$alertStationName 통과 · 다음 역 ${destinationName}에서 하차"
                }
                Text(headline, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 17.sp)
                Text(detail, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.9f))
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        Button(
            onClick = onAcknowledge,
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = DawatColors.Coral),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) {
            Text("확인했어요", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun RouteStationRow(station: TrackingViewModel.RouteStation) {
    Row(modifier = Modifier.fillMaxWidth().height(46.dp), verticalAlignment = Alignment.CenterVertically) {
        RailMarker(
            isFirst = station.isOrigin,
            isLast = station.isDestination,
            isCurrent = station.isCurrent,
            isDestination = station.isDestination,
            isPassed = station.isPassed,
            modifier = Modifier.width(36.dp).fillMaxHeight(),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            station.name,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (station.isCurrent || station.isDestination) FontWeight.Bold else FontWeight.Normal,
            color = when {
                station.isCurrent || station.isDestination -> DawatColors.OnDark
                station.isPassed -> DawatColors.OnDarkDim
                else -> DawatColors.OnDarkMuted
            },
        )
        Spacer(modifier = Modifier.weight(1f))
        when {
            station.isCurrent -> Text("현재 위치", color = DawatColors.Green, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            station.isDestination -> Text("도착", color = DawatColors.OnDarkMuted, style = MaterialTheme.typography.labelMedium)
            else -> {}
        }
    }
}

@Composable
private fun RailMarker(
    isFirst: Boolean,
    isLast: Boolean,
    isCurrent: Boolean,
    isDestination: Boolean,
    isPassed: Boolean,
    modifier: Modifier = Modifier,
) {
    val green = DawatColors.Green
    Canvas(modifier = modifier) {
        val cx = size.width / 2
        val cy = size.height / 2
        val lineStroke = 3f
        if (!isFirst) drawLine(green.copy(alpha = 0.6f), Offset(cx, 0f), Offset(cx, cy), lineStroke)
        if (!isLast) drawLine(green.copy(alpha = 0.6f), Offset(cx, cy), Offset(cx, size.height), lineStroke)

        when {
            isDestination -> {
                drawCircle(DawatColors.DarkBg, radius = 9f, center = Offset(cx, cy))
                drawCircle(Color.White, radius = 9f, center = Offset(cx, cy), style = Stroke(width = 3f))
            }
            isCurrent -> {
                drawCircle(green.copy(alpha = 0.25f), radius = 18f, center = Offset(cx, cy))
                drawCircle(green, radius = 9f, center = Offset(cx, cy))
            }
            isPassed -> drawCircle(green, radius = 6f, center = Offset(cx, cy))
            else -> drawCircle(green.copy(alpha = 0.5f), radius = 6f, center = Offset(cx, cy))
        }
    }
}
