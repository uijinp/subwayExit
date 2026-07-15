package com.jin.subwayalarm.domain.usecase

/**
 * 선형 가속도 샘플 기반 정차 카운터 (순수 로직, 설계 3-4).
 *
 * 상태: MOVING → DECEL → STOPPED(+1) → ACCEL → MOVING
 * [resync] 로 API 위치와 강제 정렬하면 카운터·상태를 리셋한다.
 */
class AccelStopCounter(
    private val moveThreshold: Float = DEFAULT_MOVE_THRESHOLD,
    private val stopThreshold: Float = DEFAULT_STOP_THRESHOLD,
    private val stopDurationMs: Long = DEFAULT_STOP_DURATION_MS,
    private val moveDurationMs: Long = DEFAULT_MOVE_DURATION_MS,
) {
    enum class Phase { MOVING, DECEL, STOPPED, ACCEL }

    var phase: Phase = Phase.MOVING
        private set
    var stopCount: Int = 0
        private set

    private var lowMagSinceMs: Long? = null
    private var highMagSinceMs: Long? = null
    private var lastSampleMs: Long = 0L

    /** 저역통과된 가속도 크기 샘플 1개 입력. 정차 커밋 시 true. */
    fun onSample(magnitude: Float, epochMillis: Long): Boolean {
        lastSampleMs = epochMillis
        var committed = false
        when (phase) {
            Phase.MOVING -> {
                if (magnitude < stopThreshold) {
                    phase = Phase.DECEL
                    lowMagSinceMs = epochMillis
                    highMagSinceMs = null
                } else {
                    highMagSinceMs = highMagSinceMs ?: epochMillis
                    lowMagSinceMs = null
                }
            }
            Phase.DECEL -> {
                if (magnitude < stopThreshold) {
                    val since = lowMagSinceMs ?: epochMillis
                    lowMagSinceMs = since
                    if (epochMillis - since >= stopDurationMs) {
                        phase = Phase.STOPPED
                        stopCount++
                        committed = true
                    }
                } else {
                    phase = Phase.MOVING
                    highMagSinceMs = epochMillis
                    lowMagSinceMs = null
                }
            }
            Phase.STOPPED -> {
                if (magnitude >= moveThreshold) {
                    phase = Phase.ACCEL
                    highMagSinceMs = epochMillis
                    lowMagSinceMs = null
                }
            }
            Phase.ACCEL -> {
                if (magnitude >= moveThreshold) {
                    val since = highMagSinceMs ?: epochMillis
                    highMagSinceMs = since
                    if (epochMillis - since >= moveDurationMs) {
                        phase = Phase.MOVING
                    }
                } else {
                    // 재가속 실패 — 정차 유지 (카운트 추가 없음)
                    phase = Phase.STOPPED
                    highMagSinceMs = null
                    lowMagSinceMs = epochMillis
                }
            }
        }
        return committed
    }

    fun resync() {
        phase = Phase.MOVING
        stopCount = 0
        lowMagSinceMs = null
        highMagSinceMs = null
    }

    companion object {
        const val DEFAULT_MOVE_THRESHOLD = 0.8f
        const val DEFAULT_STOP_THRESHOLD = 0.35f
        const val DEFAULT_STOP_DURATION_MS = 10_000L
        const val DEFAULT_MOVE_DURATION_MS = 2_000L
    }
}
