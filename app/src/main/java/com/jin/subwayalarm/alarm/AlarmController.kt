package com.jin.subwayalarm.alarm

import com.jin.subwayalarm.domain.model.AlarmSetting
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 직전역 도달 시 알람 발화·해제 조율 (4-3). EvaluateAlertUseCase 가 Trigger 를 반환하면
 * 작업순서 7(추적 루프)에서 이 컨트롤러를 호출하게 된다.
 */
@Singleton
class AlarmController @Inject constructor(
    private val vibratorController: VibratorController,
    private val notificationHelper: NotificationHelper,
) {
    fun trigger(stationName: String, setting: AlarmSetting) {
        vibratorController.start(setting.vibrationPattern, setting.repeatUntilDismissed)
        notificationHelper.showAlertNotification(stationName)
        // TODO(step9): setting.soundEnabled=false 대응은 채널 분리 필요(Android O+ 채널 소리는
        // 생성 후 앱에서 변경 불가) — UI 설정 화면과 함께 설계.
    }

    fun dismiss() {
        vibratorController.stop()
        notificationHelper.cancelAlertNotification()
    }
}
