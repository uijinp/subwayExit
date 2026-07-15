package com.jin.subwayalarm.domain.usecase

import com.jin.subwayalarm.domain.model.Direction
import com.jin.subwayalarm.domain.model.StationGraph
import com.jin.subwayalarm.domain.model.TrackingMode

/**
 * API 위치 vs 가속도계 정차 카운터 하이브리드 정책 (순수).
 *
 * - API hit: resync 카운터, 센서 pause 권고, mode=API, 위치=보정 역
 * - API miss: 센서 resume, stopCount 만큼 진행 방향 전진 추정
 */
data class HybridInput(
    val graph: StationGraph,
    val direction: Direction,
    val lastKnownStatnId: String,
    /** null = 이번 틱 API 에서 열차 소실 */
    val apiCorrectedStatnId: String?,
    val accelStopCountSinceResync: Int,
)

data class HybridResult(
    val estimatedStatnId: String,
    val mode: TrackingMode,
    /** true 면 센서를 잠시 꺼도 됨 (API 정상). */
    val pauseSensor: Boolean,
    /** true 면 카운터 resync 필요. */
    val shouldResyncCounter: Boolean,
    val stationsAdvancedByAccel: Int,
)

object HybridCoordinator {

    fun coordinate(input: HybridInput): HybridResult {
        val apiId = input.apiCorrectedStatnId
        if (apiId != null) {
            return HybridResult(
                estimatedStatnId = apiId,
                mode = TrackingMode.API,
                pauseSensor = true,
                shouldResyncCounter = true,
                stationsAdvancedByAccel = 0,
            )
        }

        var current = input.lastKnownStatnId
        var advanced = 0
        repeat(input.accelStopCountSinceResync.coerceAtLeast(0)) {
            val next = input.graph.nextStation(current, input.direction) ?: return@repeat
            current = next.statnId
            advanced++
        }
        return HybridResult(
            estimatedStatnId = current,
            mode = TrackingMode.ACCEL,
            pauseSensor = false,
            shouldResyncCounter = false,
            stationsAdvancedByAccel = advanced,
        )
    }
}
