package com.jin.subwayalarm.domain.model

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 위경도 좌표. 출발역 자동감지(Wi-Fi/기지국 측위 결과와 역 좌표 대조)에 사용 — 순수 로직.
 */
data class LatLon(val lat: Double, val lon: Double) {

    /** 두 지점 간 대원거리(Haversine, meter). */
    fun distanceMetersTo(other: LatLon): Double {
        val dLat = Math.toRadians(other.lat - lat)
        val dLon = Math.toRadians(other.lon - lon)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat)) * cos(Math.toRadians(other.lat)) * sin(dLon / 2).pow(2)
        return 2 * EARTH_RADIUS_M * asin(min(1.0, sqrt(a)))
    }

    /** 좌표 미설정(0,0) 여부 — 시드에 좌표가 없는 역을 자동감지 대상에서 제외하기 위함. */
    val isUnset: Boolean get() = lat == 0.0 && lon == 0.0

    private companion object {
        const val EARTH_RADIUS_M = 6_371_000.0
    }
}
