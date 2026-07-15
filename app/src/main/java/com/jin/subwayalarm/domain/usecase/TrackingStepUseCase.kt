package com.jin.subwayalarm.domain.usecase

import com.jin.subwayalarm.domain.model.BoardingCandidate
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
    /** 최신 폴링 결과. 네트워크 실패 시 호출측이 이번 스텝 자체를 건너뛴다. */
    val positions: List<TrainPosition>,
    val nowEpochMillis: Long,
    /** null = 노선 극성 미검증 → 탑승 열차 자동 매칭 보류. */
    val polarity: DirectionPolarity?,
    val leadStations: Int = 1,
    /** 1호선 경부 종착 등 매칭 제외 목록. */
    val excludedTerminusNames: Set<String> = emptySet(),
    /**
     * API 소실 시 가속도계 하이브리드가 추정한 현재역.
     * null 이면 소실 시 위치 유지·알람 재판정 스킵(센서 미가동).
     */
    val accelEstimatedStatnId: String? = null,
)

data class TrackingStepResult(
    val updatedSession: TripSession,
    val alert: AlertEvaluation.Trigger?,
    /** MATCHING 중이거나 재매칭용으로 노출할 후보. */
    val boardingCandidates: List<BoardingCandidate> = emptyList(),
)

/**
 * 추적 루프 한 틱의 순수 로직 (3-2). 네트워크·센서·부수효과는 호출측(TrackingService).
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
            ?: return TrackingStepResult(session, null)

        val candidates = matchBoardedTrain.candidates(
            positions = input.positions,
            lineId = session.lineId,
            originStatnId = session.originStatnId,
            direction = direction,
            polarity = polarity,
            excludedTerminusNames = input.excludedTerminusNames,
        )
        return when (candidates.size) {
            0 -> TrackingStepResult(session, null, emptyList())
            1 -> TrackingStepResult(
                session.copy(
                    boardedTrainNo = candidates.first().trainNo,
                    state = TripState.TRACKING,
                    currentStatnId = session.originStatnId,
                    lastSyncAtEpochMillis = input.nowEpochMillis,
                    mode = TrackingMode.API,
                ),
                null,
                candidates,
            )
            else -> TrackingStepResult(session, null, candidates)
        }
    }

    private fun trackBoarded(input: TrackingStepInput, direction: Direction): TrackingStepResult {
        val session = input.session
        val train = input.positions.firstOrNull { it.trainNo == session.boardedTrainNo }

        if (train == null) {
            val estimated = input.accelEstimatedStatnId
            if (estimated == null || !input.graph.contains(estimated)) {
                // 센서 추정 없음 — 마지막 위치 유지, 알람은 스킵(오발화 방지).
                return TrackingStepResult(session.copy(mode = TrackingMode.ACCEL), null)
            }
            val updated = session.copy(
                currentStatnId = estimated,
                lastSyncAtEpochMillis = input.nowEpochMillis,
                mode = TrackingMode.ACCEL,
            )
            return evaluateAndBuild(input, updated, TrainStatus.ARRIVED)
        }

        val corrected = RecptnDtCorrector.correct(
            graph = input.graph,
            rawStatnId = train.statnId,
            direction = direction,
            recptnDtEpochMillis = train.recptnDtEpochMillis,
            nowEpochMillis = input.nowEpochMillis,
            destId = session.destStatnId,
        )
        val effectiveStatus = if (corrected.stationsAdvanced > 0) TrainStatus.APPROACHING else train.status
        val updatedSession = session.copy(
            currentStatnId = corrected.statnId,
            lastSyncAtEpochMillis = input.nowEpochMillis,
            mode = TrackingMode.API,
        )
        return evaluateAndBuild(input, updatedSession, effectiveStatus)
    }

    private fun evaluateAndBuild(
        input: TrackingStepInput,
        updatedSession: TripSession,
        trainStatus: TrainStatus,
    ): TrackingStepResult {
        val session = input.session
        return when (
            val evaluation = evaluateAlert(
                graph = input.graph,
                originId = session.originStatnId,
                destId = session.destStatnId,
                currentStatnId = updatedSession.currentStatnId,
                trainStatus = trainStatus,
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

    /** TRACKING 중 재매칭용 후보 목록. */
    fun listRematchCandidates(input: TrackingStepInput): List<BoardingCandidate> {
        val polarity = input.polarity ?: return emptyList()
        val direction = input.graph.directionBetween(
            input.session.originStatnId,
            input.session.destStatnId,
        )
        // 재매칭: 현재 추정 위치(또는 출발역) 근처 후보 — 출발역 기준 + 현재역 기준 합집합
        val atOrigin = matchBoardedTrain.candidates(
            input.positions,
            input.session.lineId,
            input.session.originStatnId,
            direction,
            polarity,
            input.excludedTerminusNames,
        )
        val atCurrent = if (input.session.currentStatnId != input.session.originStatnId) {
            matchBoardedTrain.candidates(
                input.positions,
                input.session.lineId,
                input.session.currentStatnId,
                direction,
                polarity,
                input.excludedTerminusNames,
            )
        } else {
            emptyList()
        }
        // 현재 노선·방향 일치하는 모든 열차도 선택 가능하게 (이미 탑승 후 재선택)
        val allOnLine = input.positions.asSequence()
            .filter { it.lineId == input.session.lineId }
            .filter { polarity.directionOf(it.updnLine) == direction }
            .filter { terminusOk(it.terminusName, input.excludedTerminusNames) }
            .groupBy { it.trainNo }
            .map { (_, rows) ->
                val pos = rows.maxBy { it.recptnDtEpochMillis }
                BoardingCandidate(
                    trainNo = pos.trainNo,
                    statnId = pos.statnId,
                    statnName = pos.statnName,
                    status = pos.status,
                    updnLine = pos.updnLine,
                    direction = polarity.directionOf(pos.updnLine),
                    terminusName = pos.terminusName,
                    isExpress = pos.isExpress,
                    recptnDtEpochMillis = pos.recptnDtEpochMillis,
                )
            }
            .toList()
        return (atOrigin + atCurrent + allOnLine)
            .groupBy { it.trainNo }
            .map { (_, rows) -> rows.maxBy { it.recptnDtEpochMillis } }
            .sortedBy { it.trainNo }
    }

    private fun terminusOk(terminusName: String?, excluded: Set<String>): Boolean {
        if (terminusName.isNullOrBlank() || excluded.isEmpty()) return true
        val t = terminusName.trim()
        return excluded.none { ex -> t == ex || t.startsWith(ex) || t.contains(ex) }
    }
}
