package com.jin.subwayalarm.alarm

import com.jin.subwayalarm.domain.model.VibrationPattern

/**
 * 설정값(VibrationPattern·repeatUntilDismissed) → 실제 재생할 [Waveform] 변환 (순수 로직).
 */
object VibrationWaveforms {

    /** 진동(600ms)-무음(300ms) 강펄스. 선행 0 은 createWaveform 규약상 초기 지연(=즉시 시작). */
    private val STRONG_PULSE = Waveform(
        timings = listOf(0L, 600L, 300L),
        amplitudes = listOf(0, 255, 0),
        repeatFromIndex = 1,
    )

    /** [repeatUntilDismissed] 가 false 면 1회성으로 강제(repeatFromIndex=-1). */
    fun forPattern(pattern: VibrationPattern, repeatUntilDismissed: Boolean): Waveform {
        val base = when (pattern) {
            VibrationPattern.STRONG_PULSE -> STRONG_PULSE
        }
        return if (repeatUntilDismissed) base else base.copy(repeatFromIndex = -1)
    }
}
