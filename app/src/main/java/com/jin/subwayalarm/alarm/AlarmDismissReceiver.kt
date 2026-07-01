package com.jin.subwayalarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.jin.subwayalarm.data.repository.TripSessionRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 알람 알림의 "해제" 액션 버튼 처리. 앱 프로세스가 죽어있어도 동작해야 하므로
 * BroadcastReceiver 로 분리(NotificationHelper.ACTION_DISMISS_ALARM).
 */
@AndroidEntryPoint
class AlarmDismissReceiver : BroadcastReceiver() {

    @Inject lateinit var alarmController: AlarmController
    @Inject lateinit var tripSessionRepository: TripSessionRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != NotificationHelper.ACTION_DISMISS_ALARM) return
        alarmController.dismiss()

        // onReceive 는 동기 콜백이라 goAsync() 로 프로세스가 조기 종료되지 않도록 보장.
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                tripSessionRepository.clear()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
