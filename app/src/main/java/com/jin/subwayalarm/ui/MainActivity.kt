package com.jin.subwayalarm.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.jin.subwayalarm.alarm.NotificationHelper
import com.jin.subwayalarm.ui.navigation.SubwayAlarmApp
import com.jin.subwayalarm.ui.theme.SubwayAlarmTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var alarmTrigger by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        consumeAlarmExtra(intent)
        setContent {
            SubwayAlarmTheme {
                SubwayAlarmApp(alarmTrigger = alarmTrigger)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeAlarmExtra(intent)
    }

    private fun consumeAlarmExtra(intent: Intent?) {
        if (intent?.getBooleanExtra(NotificationHelper.EXTRA_SHOW_ALARM, false) == true) {
            alarmTrigger++
        }
    }
}
