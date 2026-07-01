package com.jin.subwayalarm.domain.model

/**
 * 실시간 위치 API 응답 1건의 도메인 표현 (DTO 와 분리, 추적 루프·보정 로직이 의존하는 형태).
 */
data class TrainPosition(
    val lineId: String,
    val trainNo: String,
    val statnId: String,
    val statnName: String,
    /** API 원본 상하행 코드("0"/"1"). Direction 매핑은 DirectionPolarity(노선별, 작업3 검증) 참조. */
    val updnLine: String,
    val status: TrainStatus,
    val isExpress: Boolean,
    val isLastTrain: Boolean,
    val recptnDtEpochMillis: Long,
)
