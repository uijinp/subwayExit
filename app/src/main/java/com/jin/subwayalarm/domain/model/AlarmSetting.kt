package com.jin.subwayalarm.domain.model

/**
 * 사용자 알람 설정. 알람 설정 화면에서 바꾸고 [com.jin.subwayalarm.data.repository.AlarmSettingsRepository] 에 저장된다.
 * 추적 서비스는 매 틱마다 최신 값을 읽어 [leadStations] 를 판정에, 나머지를 알람 발화에 쓴다.
 */
data class AlarmSetting(
    val vibrationPattern: VibrationPattern = VibrationPattern.STRONG_PULSE,
    /** false = 진동만(조용한 객차). 소리 없는 별도 알림 채널로 보낸다. */
    val soundEnabled: Boolean = true,
    /** 확인할 때까지 진동·소리를 반복. */
    val repeatUntilDismissed: Boolean = true,
    /** 내릴 역 몇 정거장 전에 울릴지(1~3). */
    val leadStations: Int = 1,
    /** 잠금화면 위에 전체 화면 알람을 띄울지. */
    val showOnLockScreen: Boolean = true,
) {
    companion object {
        val LEAD_STATION_CHOICES = 1..3
    }
}

enum class VibrationPattern {
    STRONG_PULSE,
}
