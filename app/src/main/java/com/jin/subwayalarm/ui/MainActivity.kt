package com.jin.subwayalarm.ui

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jin.subwayalarm.alarm.AlarmController
import com.jin.subwayalarm.domain.model.AlarmSetting
import com.jin.subwayalarm.ui.theme.SubwayAlarmTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var alarmController: AlarmController

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            // 알림 권한 거부돼도 진동은 정상 동작(4-3) — 알림은 best-effort.
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission(),
            ) { }

            SubwayAlarmTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                    PlaceholderHome(
                        modifier = Modifier.padding(padding),
                        onTestAlarm = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            alarmController.trigger("테스트역", AlarmSetting())
                        },
                        onDismissAlarm = { alarmController.dismiss() },
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaceholderHome(
    modifier: Modifier = Modifier,
    onTestAlarm: () -> Unit = {},
    onDismissAlarm: () -> Unit = {},
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("하차알람", style = MaterialTheme.typography.headlineMedium)
        Text("프로젝트 골격 구성 완료 (작업순서 1·2·5·6·8단계)", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onTestAlarm) { Text("알람 테스트 (진동 + 알림)") }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(onClick = onDismissAlarm) { Text("해제") }
    }
}

@Preview(showBackground = true)
@Composable
private fun PlaceholderHomePreview() {
    SubwayAlarmTheme { PlaceholderHome() }
}
