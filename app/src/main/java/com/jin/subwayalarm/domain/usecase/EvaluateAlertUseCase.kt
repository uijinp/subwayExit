package com.jin.subwayalarm.domain.usecase

import com.jin.subwayalarm.domain.model.StationGraph
import com.jin.subwayalarm.domain.model.TrainStatus
import javax.inject.Inject

/**
 * 직전역 판정 결과 (설계 3-1).
 */
sealed class AlertEvaluation {

    /** 알람을 울려야 함. */
    data class Trigger(val alertStationId: String, val matchedRule: Rule) : AlertEvaluation()

    /** 아직 조건 미충족. */
    data object NoTrigger : AlertEvaluation()

    /** 입력 자체가 유효하지 않음(잘못된 여정 구성 등) — 알람 대상 아님, 호출측에서 에러로 처리. */
    data class Invalid(val reason: String) : AlertEvaluation()

    enum class Rule {
        /** A) 직전역에 진입/도착 */
        AT_OR_ARRIVING_ALERT_STATION,

        /** B) 직전역의 한 정거장 전에서 출발(보수적 조기 알람) */
        DEPARTED_STATION_BEFORE_ALERT_STATION,
    }
}

/**
 * 목적지 한 역 전 트리거 판정 (설계 3-1, 그래프 기반 — 1-6).
 *
 * 순수함수: 외부 상태(시간·네트워크)에 의존하지 않아 단말 없이 반복 검증 가능(R-4 Replay 대비).
 * 중복 발화 방지(alerted 플래그)는 호출측(TripSession)이 [alreadyAlerted] 로 전달.
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

        val direction = graph.directionBetween(originId, destId)
        val alertStation = graph.stationBeforeArrival(destId, direction, leadStations)
            ?: return AlertEvaluation.Invalid(
                "직전역을 계산할 수 없습니다(목적지가 진행방향상 시발 인근): dest=$destId",
            )

        // A) 직전역 진입/도착
        if (currentStatnId == alertStation.statnId &&
            (trainStatus == TrainStatus.APPROACHING || trainStatus == TrainStatus.ARRIVED)
        ) {
            return AlertEvaluation.Trigger(alertStation.statnId, AlertEvaluation.Rule.AT_OR_ARRIVING_ALERT_STATION)
        }

        // B) 직전역의 한 정거장 전 출발 — 보수적 조기 알람
        val stationBeforeAlert = graph.previousStation(alertStation.statnId, direction)
        if (stationBeforeAlert != null &&
            currentStatnId == stationBeforeAlert.statnId &&
            trainStatus == TrainStatus.DEPARTED
        ) {
            return AlertEvaluation.Trigger(
                alertStation.statnId,
                AlertEvaluation.Rule.DEPARTED_STATION_BEFORE_ALERT_STATION,
            )
        }

        return AlertEvaluation.NoTrigger
    }
}
