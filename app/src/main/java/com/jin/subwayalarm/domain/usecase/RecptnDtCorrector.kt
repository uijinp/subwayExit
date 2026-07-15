package com.jin.subwayalarm.domain.usecase

import com.jin.subwayalarm.domain.model.Direction
import com.jin.subwayalarm.domain.model.StationGraph

data class CorrectedPosition(
    val statnId: String,
    val stationsAdvanced: Int,
)

/**
 * recptnDt 시차 보정. [destId] 가 있으면 목적 방향 경로로 전진(분기 노선 안전).
 */
object RecptnDtCorrector {

    private const val ADVANCE_THRESHOLD = 0.5

    fun correct(
        graph: StationGraph,
        rawStatnId: String,
        direction: Direction,
        recptnDtEpochMillis: Long,
        nowEpochMillis: Long,
        destId: String? = null,
    ): CorrectedPosition {
        val station = graph.station(rawStatnId)
            ?: return CorrectedPosition(rawStatnId, 0)

        val lagSeconds = (nowEpochMillis - recptnDtEpochMillis) / 1000.0
        if (lagSeconds <= 0.0) return CorrectedPosition(station.statnId, 0)

        var current = station
        var advanced = 0
        var remainingLag = lagSeconds

        while (true) {
            val next = graph.nextStation(current.statnId, direction, destId = destId) ?: break
            val segmentCost = if (direction == Direction.ASCENDING) {
                current.avgSecToNext
            } else {
                next.avgSecToNext
            }.let { if (it <= 0) current.avgSecToNext else it }
            if (segmentCost <= 0) break
            if (remainingLag / segmentCost < ADVANCE_THRESHOLD) break
            current = next
            advanced++
            remainingLag -= segmentCost
        }

        return CorrectedPosition(current.statnId, advanced)
    }
}
