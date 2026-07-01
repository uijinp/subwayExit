package com.jin.subwayalarm.domain.usecase

import com.jin.subwayalarm.domain.model.LatLon
import com.jin.subwayalarm.domain.model.Station
import javax.inject.Inject

/**
 * 현재 위치에서 가장 가까운 역을 찾는다 (출발역 자동감지, 순수 로직).
 * 좌표 미설정 역은 제외하고, [maxDistanceMeters] 를 넘으면 "근처에 역 없음"으로 간주해 null.
 */
class NearestStationUseCase @Inject constructor() {

    data class Result(val station: Station, val distanceMeters: Double)

    operator fun invoke(
        target: LatLon,
        stations: List<Station>,
        maxDistanceMeters: Double = DEFAULT_MAX_DISTANCE_M,
    ): Result? =
        stations
            .filterNot { it.latLon.isUnset }
            .map { Result(it, it.latLon.distanceMetersTo(target)) }
            .filter { it.distanceMeters <= maxDistanceMeters }
            .minByOrNull { it.distanceMeters }

    private companion object {
        // 지하철 역간 간격이 보통 700m~1.5km → 1.2km 안에 없으면 "역 근처 아님"으로 판단.
        const val DEFAULT_MAX_DISTANCE_M = 1200.0
    }
}
