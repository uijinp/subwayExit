package com.jin.subwayalarm.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.jin.subwayalarm.alarm.NotificationHelper
import com.jin.subwayalarm.data.repository.AlarmSettingsRepository
import com.jin.subwayalarm.ui.navigation.SubwayAlarmApp
import com.jin.subwayalarm.ui.theme.SubwayAlarmTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var alarmSettingsRepository: AlarmSettingsRepository

    private var alarmTrigger by mutableIntStateOf(0)

    /** 알람 화면을 잠금화면 위에 띄우는 중인지 — 회전 등으로 재생성돼도 유지. */
    private var showingOverLockScreen = false

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            consumeAlarmExtra(intent)
        } else if (savedInstanceState.getBoolean(STATE_OVER_LOCK_SCREEN)) {
            // 재생성 시 알람 인텐트는 다시 처리하지 않는다(내비게이션 상태가 이미 알람 화면을 복원함).
            showOverLockScreen(true)
        }
        setContent {
            SubwayAlarmTheme {
                SubwayAlarmApp(alarmTrigger = alarmTrigger, onAlarmFinished = ::onAlarmFinished)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeAlarmExtra(intent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_OVER_LOCK_SCREEN, showingOverLockScreen)
    }

    /**
     * 풀스크린 알림/알림 탭으로 열렸으면 알람 화면으로. 처리한 extra 는 지워서, 최근 앱 목록에서 다시 열거나
     * 프로세스 재시작으로 같은 인텐트가 다시 전달돼도 지난 알람이 또 뜨지 않게 한다.
     */
    private fun consumeAlarmExtra(intent: Intent?) {
        if (intent == null) return
        val fromHistory = intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0
        val wantsAlarm = intent.getBooleanExtra(NotificationHelper.EXTRA_SHOW_ALARM, false)
        intent.removeExtra(NotificationHelper.EXTRA_SHOW_ALARM)
        if (!wantsAlarm || fromHistory) return
        if (alarmSettingsRepository.current().showOnLockScreen) showOverLockScreen(true)
        alarmTrigger++
    }

    /** 알람 화면을 떠나면 잠금화면 위 표시를 끈다(이후 화면은 평소처럼 잠금 뒤에). */
    private fun onAlarmFinished() {
        if (showingOverLockScreen) showOverLockScreen(false)
    }

    private fun showOverLockScreen(show: Boolean) {
        showingOverLockScreen = show
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(show)
            setTurnScreenOn(show)
        } else {
            @Suppress("DEPRECATION")
            val flags = WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            if (show) window.addFlags(flags) else window.clearFlags(flags)
        }
    }

    private companion object {
        const val STATE_OVER_LOCK_SCREEN = "over_lock_screen"
    }
}
