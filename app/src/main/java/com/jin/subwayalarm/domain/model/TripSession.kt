package com.jin.subwayalarm.domain.model

enum class TripState {
    /** 탑승 열차 미확정 (MatchBoardedTrainUseCase 대기). */
    MATCHING,

    /** 탑승 열차 확정, 정상 추적 중. */
    TRACKING,

    /** 직전역 도달 트리거 발화됨(중복 발화 방지). */
    ALERTED,

    /** 종료(사용자 정지·목적지 도착 등). */
    DONE,
}

enum class TrackingMode {
    /** 실시간 API 기반. */
    API,

    /** API 응답에서 열차 소실 → 가속도계 백업(2차, 작업순서 11). */
    ACCEL,
}

/**
 * 진행 중인 여정 1건. FGS 강제 종료 후 재시작 시 이 상태로 추적을 이어받는다(4-1 사망 복구 SoT).
 * MVP 는 동시에 하나의 여정만 지원(단일 행 영속화).
 */
data class TripSession(
    val originStatnId: String,
    val destStatnId: String,
    val lineId: String,
    val currentStatnId: String,
    val lastSyncAtEpochMillis: Long,
    val boardedTrainNo: String? = null,
    val mode: TrackingMode = TrackingMode.API,
    val state: TripState = TripState.MATCHING,
)
