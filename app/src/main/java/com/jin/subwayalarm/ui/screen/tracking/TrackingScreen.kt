package com.jin.subwayalarm.ui.screen.tracking

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jin.subwayalarm.domain.model.TrackingMode
import com.jin.subwayalarm.domain.model.TripState
import com.jin.subwayalarm.tracking.TrackingService

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

    Scaffold { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // 헤더: 목적지 · 남은 정거장 · 모드
            Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                Text("목적지: ${uiState.destinationName}", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "${uiState.remainingStops} 정거장 남음",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(4.dp))
                val modeText = when {
                    uiState.state == TripState.MATCHING -> "탑승 열차 확인 중…"
                    uiState.mode == TrackingMode.API -> "실시간 추적 중"
                    else -> "추정 추적 중(신호 대기)"
                }
                Text(modeText, style = MaterialTheme.typography.bodyMedium)
            }

            // 여정 경로: 출발 → 도착, 현재 열차 위치 표시
            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 24.dp)) {
                items(uiState.route, key = { it.statnId }) { station ->
                    RouteStationRow(station)
                }
            }

            Button(
                onClick = { context.startService(TrackingService.stopIntent(context)) },
                modifier = Modifier.fillMaxWidth().padding(24.dp),
            ) {
                Text("정지")
            }
        }
    }
}

@Composable
private fun RouteStationRow(station: TrackingViewModel.RouteStation) {
    Row(
        modifier = Modifier.fillMaxWidth().height(40.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val marker = when {
            station.isCurrent -> "🚃"
            station.isPassed -> "●"
            else -> "○"
        }
        Text(
            marker,
            modifier = Modifier.width(32.dp),
            color = if (station.isPassed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        )
        val label = buildString {
            append(station.name)
            when {
                station.isOrigin -> append("  (출발)")
                station.isDestination -> append("  (도착)")
            }
        }
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (station.isCurrent) FontWeight.Bold else FontWeight.Normal,
            color = if (station.isCurrent) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
    }
}
