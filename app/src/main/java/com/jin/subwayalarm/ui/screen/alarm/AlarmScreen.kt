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
    onDismissed: () -> Unit,
    viewModel: AlarmViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(containerColor = DawatColors.Coral) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().background(DawatColors.Coral).padding(padding).padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier.size(88.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) { Text("🔔", fontSize = 40.sp) }

            Spacer(modifier = Modifier.height(28.dp))
            Text("이제 곧 내리세요!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                "다음 역 ${uiState.destinationName}에서 하차",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.92f),
            )

            Spacer(modifier = Modifier.height(40.dp))
            Button(
                onClick = {
                    viewModel.dismiss()
                    onDismissed()
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
