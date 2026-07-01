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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jin.subwayalarm.domain.model.TrackingMode
import com.jin.subwayalarm.domain.model.TripState
import com.jin.subwayalarm.tracking.TrackingService
import com.jin.subwayalarm.ui.theme.DawatColors

@Composable
fun TrackingScreen(
    onSessionEnded: () -> Unit,
    viewModel: TrackingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(uiState.isActive) {
        if (!uiState.isActive) onSessionEnded()
    }

    Scaffold(containerColor = DawatColors.DarkBg) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).background(DawatColors.DarkBg)) {
            Header(uiState)

            if (uiState.state == TripState.ALERTED) {
                AlarmBanner(
                    alertStationName = uiState.currentStationName,
                    destinationName = uiState.destinationName,
                    onAcknowledge = viewModel::acknowledgeAlarm,
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
private fun Header(uiState: TrackingViewModel.UiState) {
    Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(DawatColors.Green))
                    Spacer(modifier = Modifier.width(6.dp))
                    val statusLabel = if (uiState.state == TripState.MATCHING) "탑승 확인 중" else "추적 중"
                    Text(
                        "$statusLabel · ${uiState.lineName}",
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
private fun AlarmBanner(alertStationName: String, destinationName: String, onAcknowledge: () -> Unit) {
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
            ) { Text("🔔", fontSize = 18.sp) }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("이제 곧 내리세요!", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 17.sp)
                Text(
                    "$alertStationName 통과 · 다음 역 ${destinationName}에서 하차",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f),
                )
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
                // 도착역: 빈 원(흰 링)
                drawCircle(DawatColors.DarkBg, radius = 9f, center = Offset(cx, cy))
                drawCircle(Color.White, radius = 9f, center = Offset(cx, cy), style = Stroke(width = 3f))
            }
            isCurrent -> {
                // 현재 위치: 발광 + 큰 초록 점
                drawCircle(green.copy(alpha = 0.25f), radius = 18f, center = Offset(cx, cy))
                drawCircle(green, radius = 9f, center = Offset(cx, cy))
            }
            isPassed -> drawCircle(green, radius = 6f, center = Offset(cx, cy))
            else -> drawCircle(green.copy(alpha = 0.5f), radius = 6f, center = Offset(cx, cy))
        }
    }
}
