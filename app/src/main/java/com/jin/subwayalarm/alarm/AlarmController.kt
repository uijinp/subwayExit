package com.jin.subwayalarm.alarm

import com.jin.subwayalarm.domain.model.AlarmSetting
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 알람 발화·해제 조율 (4-3). 추적 서비스가 판정 Trigger 를 받으면 호출한다.
 * 소리 여부·반복·잠금화면 표시는 사용자 설정([AlarmSetting])을 따른다.
 */
@Singleton
class AlarmController @Inject constructor(
    private val vibratorController: VibratorController,
    private val notificationHelper: NotificationHelper,
) {
    fun trigger(content: AlarmContent, setting: AlarmSetting) {
        vibratorController.start(setting.vibrationPattern, setting.repeatUntilDismissed)
        notificationHelper.showAlertNotification(content, setting)
    }

    fun dismiss() {
        vibratorController.stop()
        notificationHelper.cancelAlertNotification()
    }
}
