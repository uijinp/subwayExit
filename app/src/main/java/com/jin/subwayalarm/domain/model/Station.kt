package com.jin.subwayalarm.domain.model

/**
 * 역 도메인 모델 (Room 엔티티와 분리).
 *
 * @param statnId      역 ID (실시간 API 의 statnId 와 동일 키)
 * @param lineId       노선 ID (API subwayId, 예: 7호선 = "1007")
 * @param name         역명
 * @param order        노선 정렬 순번 (ASCENDING 방향 기준 0..N)
 * @param avgSecToNext 다음(ASCENDING) 역까지 평균 소요시간(초). recptnDt 보정용. 종착은 0.
 * @param nextStatnId  ASCENDING 방향 인접 역 (그래프 확장용, LINEAR 에선 order 로 파생 가능)
 * @param prevStatnId  DESCENDING 방향 인접 역
 */
data class Station(
    val statnId: String,
    val lineId: String,
    val name: String,
    val order: Int,
    val avgSecToNext: Int,
    val nextStatnId: String? = null,
    val prevStatnId: String? = null,
)
