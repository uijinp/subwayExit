package com.jin.subwayalarm.domain.model

/**
 * 실시간 위치 API 의 trainSttus 코드.
 * ⚠️ 코드값은 설계 기준값 — 작업순서 3(실측 검증 스파이크)에서 재검증할 것.
 */
enum class TrainStatus(val code: Int) {
    /** 0: 진입 */
    APPROACHING(0),

    /** 1: 도착 */
    ARRIVED(1),

    /** 2: 출발 */
    DEPARTED(2),

    /** 3: 전역 출발(이전 역에서 출발, 현재역 진입 전) — MVP 알람 판정에는 미사용 */
    DEPARTED_PREVIOUS_STATION(3);

    companion object {
        fun fromCode(code: Int): TrainStatus? = entries.find { it.code == code }
    }
}
