package com.jin.subwayalarm.ui.screen.alarm

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jin.subwayalarm.ui.theme.DawatColors

@Composable
fun AlarmScreen(
    /** isTransfer: 다음 구간이 있어 추적 화면으로 이어가야 하는지, 없으면(마지막) 홈으로. */
    onDismissed: (isTransfer: Boolean) -> Unit,
    viewModel: AlarmViewModel = hiltViewModel(),
) {
    val loadedState by viewModel.uiState.collectAsStateWithLifecycle()
    // 로딩 전(null)에는 아무것도 그리지 않는다 — 기본값 isTransfer=false 로 환승 여정을 잘못 끝내는 것 방지.
    val uiState = loadedState ?: return

    Scaffold(containerColor = DawatColors.Coral) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().background(DawatColors.Coral).padding(padding).padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier.size(88.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) { Text(if (uiState.isTransfer) "🔄" else "🔔", fontSize = 40.sp) }

            Spacer(modifier = Modifier.height(28.dp))
            val headline = if (uiState.isTransfer) "환승하세요!" else "이제 곧 내리세요!"
            Text(headline, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(10.dp))
            val detail = if (uiState.isTransfer) {
                "여기서 내려 ${uiState.nextLineName}으로 갈아타세요"
            } else {
                "다음 역 ${uiState.destinationName}에서 하차"
            }
            Text(detail, style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = 0.92f))

            Spacer(modifier = Modifier.height(40.dp))
            Button(
                onClick = {
                    val isTransfer = uiState.isTransfer
                    viewModel.dismiss()
                    onDismissed(isTransfer)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = DawatColors.Coral),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                Text("확인했어요", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}
