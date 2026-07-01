package com.jin.subwayalarm.domain.usecase

import com.jin.subwayalarm.domain.model.Direction
import com.jin.subwayalarm.domain.model.DirectionPolarity
import com.jin.subwayalarm.domain.model.StationGraph
import com.jin.subwayalarm.domain.model.TrackingMode
import com.jin.subwayalarm.domain.model.TrainPosition
import com.jin.subwayalarm.domain.model.TrainStatus
import com.jin.subwayalarm.domain.model.TripSession
import com.jin.subwayalarm.domain.model.TripState
import javax.inject.Inject

data class TrackingStepInput(
    val graph: StationGraph,
    val session: TripSession,
    /** 최신 폴링 결과. 네트워크 실패 시 빈 리스트가 아니라 호출측이 이번 스텝 자체를 건너뛴다. */
    val positions: List<TrainPosition>,
    val nowEpochMillis: Long,
    /** null = 노선 극성 미검증(작업3) → 탑승 열차 자동 매칭 보류. */
    val polarity: DirectionPolarity?,
    val leadStations: Int = 1,
)

data class TrackingStepResult(
    val updatedSession: TripSession,
    val alert: AlertEvaluation.Trigger?,
)

/**
 * 추적 루프 한 틱의 순수 로직 (3-2). 네트워크 폴링·딜레이·부수효과(알람 발화·DB 저장)는
 * 호출측(TrackingService)이 담당 — 이 함수는 세션 전이만 계산해 단말 없이 반복 검증 가능(R-4).
 */
class TrackingStepUseCase @Inject constructor(
    private val evaluateAlert: EvaluateAlertUseCase,
    private val matchBoardedTrain: MatchBoardedTrainUseCase,
) {
    operator fun invoke(input: TrackingStepInput): TrackingStepResult {
        val session = input.session
        if (session.state == TripState.ALERTED || session.state == TripState.DONE) {
            return TrackingStepResult(session, null)
        }

        val direction = input.graph.directionBetween(session.originStatnId, session.destStatnId)

        if (session.boardedTrainNo == null) {
            return matchBoarding(input, direction)
        }

        return trackBoarded(input, direction)
    }

    private fun matchBoarding(input: TrackingStepInput, direction: Direction): TrackingStepResult {
        val session = input.session
        val polarity = input.polarity
            ?: return TrackingStepResult(session, null) // 극성 미검증 — 수동 확정 대기(작업9/10)

        return when (
            val match = matchBoardedTrain(
                positions = input.positions,
                lineId = session.lineId,
                originStatnId = session.originStatnId,
                direction = direction,
                polarity = polarity,
            )
        ) {
            is BoardingMatch.Matched -> TrackingStepResult(
                session.copy(
                    boardedTrainNo = match.trainNo,
                    state = TripState.TRACKING,
                    currentStatnId = session.originStatnId,
                    lastSyncAtEpochMillis = input.nowEpochMillis,
                    mode = TrackingMode.API,
                ),
                null,
            )
            BoardingMatch.NoCandidate, is BoardingMatch.Ambiguous -> TrackingStepResult(session, null)
        }
    }

    private fun trackBoarded(input: TrackingStepInput, direction: Direction): TrackingStepResult {
        val session = input.session
        val train = input.positions.firstOrNull { it.trainNo == session.boardedTrainNo }

        if (train == null) {
            // 응답에서 소실(지하 등) — 2차 가속도계(작업11) 미구현이라 마지막 위치를 유지한 채
            // mode 만 ACCEL 로 표시하고, 이번 스텝은 알람 재판정을 건너뛴다(1-4/11 참조).
            return TrackingStepResult(session.copy(mode = TrackingMode.ACCEL), null)
        }

        val corrected = RecptnDtCorrector.correct(
            graph = input.graph,
            rawStatnId = train.statnId,
            direction = direction,
            recptnDtEpochMillis = train.recptnDtEpochMillis,
            nowEpochMillis = input.nowEpochMillis,
        )
        // 보정으로 역을 앞당겼다면 실측 상태를 알 수 없으니, 조기 알람 규칙(1-2)에 따라
        // "진입" 으로 간주해 규칙 A 판정에 태운다.
        val effectiveStatus = if (corrected.stationsAdvanced > 0) TrainStatus.APPROACHING else train.status

        val updatedSession = session.copy(
            currentStatnId = corrected.statnId,
            lastSyncAtEpochMillis = input.nowEpochMillis,
            mode = TrackingMode.API,
        )

        return when (
            val evaluation = evaluateAlert(
                graph = input.graph,
                originId = session.originStatnId,
                destId = session.destStatnId,
                currentStatnId = updatedSession.currentStatnId,
                trainStatus = effectiveStatus,
                leadStations = input.leadStations,
                alreadyAlerted = false,
            )
        ) {
            is AlertEvaluation.Trigger -> TrackingStepResult(
                updatedSession.copy(state = TripState.ALERTED),
                evaluation,
            )
            AlertEvaluation.NoTrigger, is AlertEvaluation.Invalid -> TrackingStepResult(updatedSession, null)
        }
    }
}
