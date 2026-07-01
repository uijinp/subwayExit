package com.jin.subwayalarm.alarm

/**
 * VibrationEffect.createWaveform(timings, amplitudes, repeat) 에 그대로 넘길 수 있는 파형.
 * LongArray/IntArray 대신 List 로 표현해 구조적 equals 를 갖게 해 단위테스트 가능하게 한다.
 */
data class Waveform(
    val timings: List<Long>,
    val amplitudes: List<Int>,
    /** VibrationEffect.createWaveform 의 repeat 인덱스. -1 이면 1회성(반복 없음). */
    val repeatFromIndex: Int,
) {
    init {
        require(timings.isNotEmpty()) { "timings 가 비어 있습니다." }
        require(timings.size == amplitudes.size) {
            "timings·amplitudes 길이가 같아야 합니다: ${timings.size} vs ${amplitudes.size}"
        }
        require(repeatFromIndex == -1 || repeatFromIndex in timings.indices) {
            "repeatFromIndex 범위 초과: $repeatFromIndex (size=${timings.size})"
        }
    }
}
