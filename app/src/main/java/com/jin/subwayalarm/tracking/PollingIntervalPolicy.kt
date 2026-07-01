package com.jin.subwayalarm.tracking

/**
 * 거리 적응형 폴링 주기 + 레이트리밋 백오프 (2-7). 일일 호출 한도(R-1) 절약이 목적.
 */
object PollingIntervalPolicy {

    /** 목적지까지 남은 역 수에 따른 폴링 주기(초). */
    fun intervalSeconds(remainingStops: Int): Long = when {
        remainingStops > 4 -> 75L
        remainingStops >= 2 -> 30L
        else -> 15L
    }

    private const val BASE_BACKOFF_SECONDS = 15L
    private const val MAX_BACKOFF_SECONDS = 600L

    // shl 오버플로 방지용 안전 상한일 뿐, 실질 상한은 MAX_BACKOFF_SECONDS(coerceAtMost)가 담당.
    private const val SHIFT_SAFETY_CAP = 10

    /** 연속 실패 횟수에 따른 지수 백오프(초): 15,30,60,120,240,480,600(상한),... 429/네트워크 오류 시 사용. */
    fun backoffSeconds(consecutiveFailures: Int): Long {
        if (consecutiveFailures <= 0) return 0L
        val shift = (consecutiveFailures - 1).coerceAtMost(SHIFT_SAFETY_CAP)
        return (BASE_BACKOFF_SECONDS shl shift).coerceAtMost(MAX_BACKOFF_SECONDS)
    }
}
