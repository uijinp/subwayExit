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

    /**
     * 탑승 확인(MATCHING) 중 폴링 주기 — 매칭은 "출발역에 서 있는 열차"를 폴링이 목격해야
     * 성립하는데 정차 창이 약 30초라, 거리 기반 주기(최대 75초)로는 절반 이상 놓친다.
     * 매칭 단계는 짧게 끝나므로 짧은 고정 주기가 한도 부담도 작다.
     */
    const val MATCHING_INTERVAL_SECONDS = 15L

    private const val BASE_BACKOFF_SECONDS = 15L

    // 추적은 시간에 민감하다 — 백오프가 너무 길면 알람 창을 통째로 놓친다(만회 규칙이 있어도
    // 열차가 목적지를 지나버린 뒤가 될 수 있음). 지하 터널의 일시 통신 두절은 다음 역에서
    // 풀리는 게 보통이라 2분이면 충분히 회복을 기다린다.
    private const val MAX_BACKOFF_SECONDS = 120L

    // shl 오버플로 방지용 안전 상한일 뿐, 실질 상한은 MAX_BACKOFF_SECONDS(coerceAtMost)가 담당.
    private const val SHIFT_SAFETY_CAP = 10

    /** 연속 실패 횟수에 따른 지수 백오프(초): 15,30,60,120(상한). 429/네트워크 오류 시 사용. */
    fun backoffSeconds(consecutiveFailures: Int): Long {
        if (consecutiveFailures <= 0) return 0L
        val shift = (consecutiveFailures - 1).coerceAtMost(SHIFT_SAFETY_CAP)
        return (BASE_BACKOFF_SECONDS shl shift).coerceAtMost(MAX_BACKOFF_SECONDS)
    }
}
