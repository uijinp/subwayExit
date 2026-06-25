package com.jin.subwayalarm.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jin.subwayalarm.ui.theme.SubwayAlarmTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            SubwayAlarmTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                    PlaceholderHome(Modifier.padding(padding))
                }
            }
        }
    }
}

@Composable
private fun PlaceholderHome(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("하차알람", style = MaterialTheme.typography.headlineMedium)
        Text("프로젝트 골격 구성 완료 (작업순서 1단계)", style = MaterialTheme.typography.bodyMedium)
    }
}

@Preview(showBackground = true)
@Composable
private fun PlaceholderHomePreview() {
    SubwayAlarmTheme { PlaceholderHome() }
}
