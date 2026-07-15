package com.jin.subwayalarm.domain.usecase

import com.jin.subwayalarm.domain.model.DirectionPolarity
import com.jin.subwayalarm.domain.model.Station
import com.jin.subwayalarm.domain.model.StationGraph
import com.jin.subwayalarm.domain.model.TrackingMode
import com.jin.subwayalarm.domain.model.TrainPosition
import com.jin.subwayalarm.domain.model.TrainStatus
import com.jin.subwayalarm.domain.model.TripSession
import com.jin.subwayalarm.domain.model.TripState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class TrackingStepUseCaseTest {

    private val useCase = TrackingStepUseCase(EvaluateAlertUseCase(), MatchBoardedTrainUseCase())
    private val polarity = DirectionPolarity(ascendingUpdnLine = "0")

    // A(0) - B(1) - C(2) - D(3) - E(4), origin=A dest=D → 직전역=C
    private fun graph(avgSec: Int = 1000): StationGraph {
        val names = listOf("A", "B", "C", "D", "E")
        return StationGraph(names.mapIndexed { i, n -> Station(n, "L", n, i, avgSec) })
    }

    private fun matchingSession(now: Long) = TripSession(
        originStatnId = "A", destStatnId = "D", lineId = "L",
        currentStatnId = "A", lastSyncAtEpochMillis = now, state = TripState.MATCHING,
    )

    private fun trackingSession(now: Long, currentStatnId: String = "A", trainNo: String = "T1") = TripSession(
        originStatnId = "A", destStatnId = "D", lineId = "L",
        currentStatnId = currentStatnId, lastSyncAtEpochMillis = now,
        boardedTrainNo = trainNo, state = TripState.TRACKING,
    )

    private fun position(
        trainNo: String,
        statnId: String,
        status: TrainStatus,
        updnLine: String = "0",
        recptnDt: Long,
    ) = TrainPosition(
        lineId = "L", trainNo = trainNo, statnId = statnId, statnName = statnId,
        updnLine = updnLine, status = status, isExpress = false, isLastTrain = false,
        recptnDtEpochMillis = recptnDt,
    )

    @Test
    fun `극성 미검증이면 매칭 단계에서 세션 그대로`() {
        val now = 10_000L
        val input = TrackingStepInput(graph(), matchingSession(now), positions = emptyList(), nowEpochMillis = now, polarity = null)
        val result = useCase(input)
        assertEquals(matchingSession(now), result.updatedSession)
        assertNull(result.alert)
    }

    @Test
    fun `매칭 단계 - 후보 1개면 TRACKING 으로 전이`() {
        val now = 10_000L
        val positions = listOf(position("T1", "A", TrainStatus.ARRIVED, recptnDt = now))
        val input = TrackingStepInput(graph(), matchingSession(now), positions, now, polarity)
        val result = useCase(input)
        assertEquals("T1", result.updatedSession.boardedTrainNo)
        assertEquals(TripState.TRACKING, result.updatedSession.state)
        assertEquals("A", result.updatedSession.currentStatnId)
        assertNull(result.alert)
    }

    @Test
    fun `매칭 단계 - 후보 없으면 MATCHING 유지`() {
        val now = 10_000L
        val input = TrackingStepInput(graph(), matchingSession(now), positions = emptyList(), nowEpochMillis = now, polarity = polarity)
        val result = useCase(input)
        assertEquals(TripState.MATCHING, result.updatedSession.state)
        assertNull(result.updatedSession.boardedTrainNo)
    }

    @Test
    fun `추적 단계 - 직전역(C) 도착이면 트리거`() {
        val now = 10_000L
        val positions = listOf(position("T1", "C", TrainStatus.ARRIVED, recptnDt = now))
        val input = TrackingStepInput(graph(), trackingSession(now, currentStatnId = "B"), positions, now, polarity)
        val result = useCase(input)
        assertNotNull(result.alert)
        assertEquals("C", result.alert!!.alertStationId)
        assertEquals(TripState.ALERTED, result.updatedSession.state)
    }

    @Test
    fun `추적 단계 - 직전역에서 출발 상태로 목격되면 만회 규칙으로 트리거`() {
        // 직전역에서 이미 출발했다면 다음 정차역이 목적지 — 여기서 안 울리면 기회가 없다(판정 만회 규칙 C).
        val now = 10_000L
        val positions = listOf(position("T1", "C", TrainStatus.DEPARTED, recptnDt = now))
        val input = TrackingStepInput(graph(), trackingSession(now, currentStatnId = "B"), positions, now, polarity)
        val result = useCase(input)
        assertNotNull(result.alert)
        assertEquals("C", result.alert!!.alertStationId)
        assertEquals(TripState.ALERTED, result.updatedSession.state)
        assertEquals("C", result.updatedSession.currentStatnId)
    }

    @Test
    fun `추적 단계 - 직전역 한 정거장 전(B) 출발이면 보수적 조기 트리거`() {
        val now = 10_000L
        val positions = listOf(position("T1", "B", TrainStatus.DEPARTED, recptnDt = now))
        val input = TrackingStepInput(graph(), trackingSession(now, currentStatnId = "A"), positions, now, polarity)
        val result = useCase(input)
        assertNotNull(result.alert)
        assertEquals("C", result.alert!!.alertStationId)
    }

    @Test
    fun `추적 단계 - 열차가 응답에서 사라지면 ACCEL 모드 표시, 위치는 유지`() {
        val now = 10_000L
        val input = TrackingStepInput(
            graph(),
            trackingSession(now, currentStatnId = "B"),
            positions = emptyList(),
            nowEpochMillis = now,
            polarity = polarity,
        )
        val result = useCase(input)
        assertNull(result.alert)
        assertEquals(TrackingMode.ACCEL, result.updatedSession.mode)
        assertEquals("B", result.updatedSession.currentStatnId)
    }

    @Test
    fun `추적 단계 - API 소실이어도 가속도 추정 역이 있으면 재판정`() {
        val now = 10_000L
        val input = TrackingStepInput(
            graph(),
            trackingSession(now, currentStatnId = "B"),
            positions = emptyList(),
            nowEpochMillis = now,
            polarity = polarity,
            accelEstimatedStatnId = "C",
        )
        val result = useCase(input)
        assertNotNull(result.alert)
        assertEquals(TripState.ALERTED, result.updatedSession.state)
        assertEquals(TrackingMode.ACCEL, result.updatedSession.mode)
    }

    @Test
    fun `매칭 단계 - 후보 2개면 MATCHING 유지하고 후보 목록 반환`() {
        val now = 10_000L
        val positions = listOf(
            position("T1", "A", TrainStatus.ARRIVED, recptnDt = now),
            position("T2", "A", TrainStatus.APPROACHING, recptnDt = now),
        )
        val input = TrackingStepInput(graph(), matchingSession(now), positions, now, polarity)
        val result = useCase(input)
        assertEquals(TripState.MATCHING, result.updatedSession.state)
        assertNull(result.updatedSession.boardedTrainNo)
        assertEquals(2, result.boardingCandidates.size)
    }

    @Test
    fun `이미 ALERTED 면 조건 충족해도 재트리거 없음`() {
        val now = 10_000L
        val alerted = trackingSession(now, currentStatnId = "C").copy(state = TripState.ALERTED)
        val positions = listOf(position("T1", "C", TrainStatus.ARRIVED, recptnDt = now))
        val input = TrackingStepInput(graph(), alerted, positions, now, polarity)
        val result = useCase(input)
        assertNull(result.alert)
        assertEquals(alerted, result.updatedSession)
    }

    @Test
    fun `DONE 상태면 그대로 유지`() {
        val now = 10_000L
        val done = trackingSession(now).copy(state = TripState.DONE)
        val input = TrackingStepInput(graph(), done, positions = emptyList(), nowEpochMillis = now, polarity = polarity)
        val result = useCase(input)
        assertEquals(done, result.updatedSession)
        assertNull(result.alert)
    }

    @Test
    fun `recptnDt 시차 보정으로 앞당겨지면 조기(APPROACHING 간주) 트리거`() {
        // avgSecToNext=100, B 에서 lag=50s(=50%) → C 로 보정, 상태 불명이라 APPROACHING 간주 → 규칙 A 트리거
        val now = 50_000L // recptnDt=0 이므로 lag=50s
        val positions = listOf(position("T1", "B", TrainStatus.DEPARTED, recptnDt = 0L))
        val input = TrackingStepInput(graph(avgSec = 100), trackingSession(now, currentStatnId = "A"), positions, now, polarity)
        val result = useCase(input)
        assertNotNull(result.alert)
        assertEquals("C", result.alert!!.alertStationId)
        assertEquals("C", result.updatedSession.currentStatnId)
    }
}
