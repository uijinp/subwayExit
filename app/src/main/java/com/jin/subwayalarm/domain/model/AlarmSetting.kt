package com.jin.subwayalarm.domain.model

/**
 * 사용자 알람 설정. 영속화·UI 배선은 작업순서 9(UI)에서.
 * 지금은 [com.jin.subwayalarm.alarm.AlarmController] 가 기본값으로 바로 사용.
 */
data class AlarmSetting(
    val vibrationPattern: VibrationPattern = VibrationPattern.STRONG_PULSE,
    val soundEnabled: Boolean = true,
    val repeatUntilDismissed: Boolean = true,
    val leadStations: Int = 1,
)

enum class VibrationPattern {
    STRONG_PULSE,
}
