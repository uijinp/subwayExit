package com.jin.subwayalarm.ui.screen.tracking

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (uiState.state == TripState.MATCHING) {
                Text("탑승 열차 확인 중…", style = MaterialTheme.typography.titleMedium)
            } else {
                Text("목적지: ${uiState.destinationName}", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text("현재 추정역: ${uiState.currentStationName}", style = MaterialTheme.typography.bodyLarge)
                Text("남은 역: ${uiState.remainingStops}개", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "모드: ${if (uiState.mode == TrackingMode.API) "실시간" else "추정(가속도계 대기)"}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedButton(onClick = { context.startService(TrackingService.stopIntent(context)) }) {
                Text("정지")
            }
        }
    }
}
