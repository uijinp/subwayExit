package com.jin.subwayalarm.domain.model

/**
 * 노선 메타 도메인 모델.
 *
 * @param lineId           노선 ID (API subwayId, 예: "1007")
 * @param lineName         노선명 (API URL·키, 예: "1호선" — 표시와 다를 수 있음)
 * @param displayName      UI 표시명 (예: "1호선 (경인 청량리–인천)")
 * @param supportsRealtime 실시간 위치 API 지원 여부
 * @param topology         노선 위상
 * @param directionPolarity updnLine↔Direction 매핑
 * @param excludedTerminusNames 탑승 매칭 시 제외할 종착역명(부분일치). 1호선 경부 분기 필터 등.
 */
data class LineMeta(
    val lineId: String,
    val lineName: String,
    val supportsRealtime: Boolean,
    val topology: LineTopology,
    val directionPolarity: DirectionPolarity? = null,
    val displayName: String = lineName,
    val excludedTerminusNames: Set<String> = emptySet(),
)
