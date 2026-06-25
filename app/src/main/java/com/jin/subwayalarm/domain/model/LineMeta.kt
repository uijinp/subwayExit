package com.jin.subwayalarm.domain.model

/**
 * 노선 메타 도메인 모델.
 *
 * @param lineId           노선 ID (API subwayId, 예: "1007")
 * @param lineName         노선명 (URL·표시용, 예: "7호선")
 * @param supportsRealtime 실시간 위치 API 지원 여부 (서울 외 구간 false)
 * @param topology         노선 위상
 */
data class LineMeta(
    val lineId: String,
    val lineName: String,
    val supportsRealtime: Boolean,
    val topology: LineTopology,
)
