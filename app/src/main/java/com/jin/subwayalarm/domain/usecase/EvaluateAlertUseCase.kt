package com.jin.subwayalarm.domain.usecase

import com.jin.subwayalarm.domain.model.StationGraph
import com.jin.subwayalarm.domain.model.TrainStatus
import javax.inject.Inject

/**
 * 직전역 판정 결과 (설계 3-1).
 */
sealed class AlertEvaluation {

    data class Trigger(val alertStationId: String, val matchedRule: Rule) : AlertEvaluation()

    data object NoTrigger : AlertEvaluation()

    data class Invalid(val reason: String) : AlertEvaluation()

    enum class Rule {
        AT_OR_ARRIVING_ALERT_STATION,
        DEPARTED_STATION_BEFORE_ALERT_STATION,
        AT_OR_PAST_ALERT_STATION,
    }
}

/**
 * 목적지 한 역 전 트리거 판정 — origin→dest 최단 경로 기준 (LINEAR·BRANCHED 공통).
 */
class EvaluateAlertUseCase @Inject constructor() {

    operator fun invoke(
        graph: StationGraph,
        originId: String,
        destId: String,
        currentStatnId: String,
        trainStatus: TrainStatus,
        leadStations: Int = 1,
        alreadyAlerted: Boolean = false,
    ): AlertEvaluation {
        if (alreadyAlerted) return AlertEvaluation.NoTrigger

        if (!graph.isValidTrip(originId, destId)) {
            return AlertEvaluation.Invalid("출발역/목적역이 유효하지 않습니다: origin=$originId dest=$destId")
        }
        if (!graph.contains(currentStatnId)) {
            return AlertEvaluation.Invalid("현재역이 노선 그래프에 없습니다: $currentStatnId")
        }

        val path = graph.path(originId, destId)
            ?: return AlertEvaluation.Invalid("경로 없음: $originId → $destId")
        val direction = graph.directionBetween(originId, destId)
        val alertStation = graph.stationBeforeArrival(destId, direction, leadStations, originId = originId)
            ?: return AlertEvaluation.Invalid(
                "직전역을 계산할 수 없습니다(목적지가 진행방향상 시발 인근): dest=$destId",
            )

        // A) 직전역 진입/도착
        if (currentStatnId == alertStation.statnId &&
            (trainStatus == TrainStatus.APPROACHING || trainStatus == TrainStatus.ARRIVED)
        ) {
            return AlertEvaluation.Trigger(alertStation.statnId, AlertEvaluation.Rule.AT_OR_ARRIVING_ALERT_STATION)
        }

        // B) 직전역의 한 정거장 전 출발 — 경로 위 역
        val stationBeforeAlert =
            graph.stationBeforeArrival(destId, direction, leadStations + 1, originId = originId)
        if (stationBeforeAlert != null &&
            currentStatnId == stationBeforeAlert.statnId &&
            trainStatus == TrainStatus.DEPARTED
        ) {
            return AlertEvaluation.Trigger(
                alertStation.statnId,
                AlertEvaluation.Rule.DEPARTED_STATION_BEFORE_ALERT_STATION,
            )
        }

        // C) origin→current 경로 위에 직전역이 이미 있으면(도달·통과·목적지 너머) 발화.
        //    목적지 너머 역도 path(origin, current) 에 직전역이 포함되면 만회 가능.
        val pathToCurrent = graph.path(originId, currentStatnId)
        val alertIdxOnWay = pathToCurrent?.indexOfFirst { it.statnId == alertStation.statnId } ?: -1
        val alertReached = alertIdxOnWay >= 0
        if (alertReached && (currentStatnId != originId || trainStatus == TrainStatus.DEPARTED)) {
            return AlertEvaluation.Trigger(alertStation.statnId, AlertEvaluation.Rule.AT_OR_PAST_ALERT_STATION)
        }

        return AlertEvaluation.NoTrigger
    }
}
