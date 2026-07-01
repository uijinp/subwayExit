package com.jin.subwayalarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * 알람 알림의 "해제" 액션 버튼 처리. 앱 프로세스가 죽어있어도 동작해야 하므로
 * BroadcastReceiver 로 분리(NotificationHelper.ACTION_DISMISS_ALARM).
 */
@AndroidEntryPoint
class AlarmDismissReceiver : BroadcastReceiver() {

    @Inject lateinit var alarmController: AlarmController

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != NotificationHelper.ACTION_DISMISS_ALARM) return
        alarmController.dismiss()
        // TODO(step7): TripSession 상태를 ALERTED→DONE 으로 갱신, TrackingService 종료(stopSelf)
    }
}
