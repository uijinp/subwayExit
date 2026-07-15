package com.jin.subwayalarm.replay

import com.jin.subwayalarm.domain.model.DirectionPolarity
import com.jin.subwayalarm.domain.model.StationGraph
import com.jin.subwayalarm.domain.model.TrackingMode
import com.jin.subwayalarm.domain.model.TripSession
import com.jin.subwayalarm.domain.model.TripState
import com.jin.subwayalarm.domain.usecase.AlertEvaluation
import com.jin.subwayalarm.domain.usecase.EvaluateAlertUseCase
import com.jin.subwayalarm.domain.usecase.MatchBoardedTrainUseCase
import com.jin.subwayalarm.domain.usecase.TrackingStepInput
import com.jin.subwayalarm.domain.usecase.TrackingStepResult
import com.jin.subwayalarm.domain.usecase.TrackingStepUseCase

data class ReplayRunLog(
    val frameIndex: Int,
    val tOffsetSec: Long,
    val session: TripSession,
    val alert: AlertEvaluation.Trigger?,
)

data class ReplayRunResult(
    val finalSession: TripSession,
    val logs: List<ReplayRunLog>,
    val triggered: Boolean,
)

/**
 * 단말/네트워크 없이 TrackingStep 시계열 재생 (헤드리스).
 */
class ReplayTripRunner(
    private val trackingStep: TrackingStepUseCase = TrackingStepUseCase(
        EvaluateAlertUseCase(),
        MatchBoardedTrainUseCase(),
    ),
) {
    fun run(
        fixture: ReplayFixture,
        graph: StationGraph,
        polarity: DirectionPolarity?,
        excludedTerminusNames: Set<String> = emptySet(),
        clock: ReplayClock = ReplayClock(),
        /** 프레임 인덱스 → API 소실 시 가속도 추정 역 (테스트용). */
        accelEstimateAtFrame: (Int, TripSession) -> String? = { _, _ -> null },
    ): ReplayRunResult {
        var session = TripSession(
            originStatnId = fixture.trip.originStatnId,
            destStatnId = fixture.trip.destStatnId,
            lineId = fixture.meta.lineId,
            currentStatnId = fixture.trip.originStatnId,
            lastSyncAtEpochMillis = clock.nowAt(0),
            state = TripState.MATCHING,
            mode = TrackingMode.API,
        )
        val logs = mutableListOf<ReplayRunLog>()
        var lastAlert: AlertEvaluation.Trigger? = null

        fixture.frames.forEachIndexed { index, frame ->
            if (session.state == TripState.ALERTED || session.state == TripState.DONE) return@forEachIndexed
            val now = clock.nowAt(frame.tOffsetSec)
            val positions = frame.positions.mapNotNull { it.toDomain(fixture.meta.lineId) }
            val result: TrackingStepResult = trackingStep(
                TrackingStepInput(
                    graph = graph,
                    session = session,
                    positions = positions,
                    nowEpochMillis = now,
                    polarity = polarity,
                    leadStations = fixture.trip.leadStations,
                    excludedTerminusNames = excludedTerminusNames,
                    accelEstimatedStatnId = accelEstimateAtFrame(index, session),
                ),
            )
            session = result.updatedSession
            lastAlert = result.alert ?: lastAlert
            logs.add(ReplayRunLog(index, frame.tOffsetSec, session, result.alert))
        }

        return ReplayRunResult(
            finalSession = session,
            logs = logs,
            triggered = lastAlert != null || session.state == TripState.ALERTED,
        )
    }
}
