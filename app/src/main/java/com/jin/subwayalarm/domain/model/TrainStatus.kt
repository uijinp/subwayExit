package com.jin.subwayalarm.domain.model

/**
 * 실시간 위치 API 의 trainSttus 코드.
 * 작업순서 3(2026-07-01) 실측: 0/1/2 는 7호선 실응답에서 관측 확인(0=진입 6건, 1=도착 26건,
 * 2=출발 12건, 44건 샘플). 3(전역출발)은 이번 샘플엔 미관측이나 코드 정의는 유지.
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
