package com.jin.subwayalarm.location

import com.jin.subwayalarm.domain.model.LatLon

/**
 * 현재 위치 1회 조회(출발역 자동감지). 실제 추적은 실시간 API 이므로 위치는 오직 이 용도만.
 */
interface LocationProvider {
    /** Wi-Fi/기지국 기반 현재 위치. 권한 없음·실패·시간초과 시 null. */
    suspend fun currentLatLon(): LatLon?
}
