package com.jin.subwayalarm.replay

import com.jin.subwayalarm.domain.model.DirectionPolarity
import com.jin.subwayalarm.domain.model.Station
import com.jin.subwayalarm.domain.model.StationGraph
import com.jin.subwayalarm.domain.model.TripState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReplayTripRunnerTest {

    // A-B-C-D-E, origin=A dest=D → alert at C
    private val graph = StationGraph(
        listOf("A", "B", "C", "D", "E").mapIndexed { i, n ->
            Station(statnId = n, lineId = "L", name = n, order = i, avgSecToNext = 1000)
        },
    )
    private val polarity = DirectionPolarity("0")

    @Test
    fun `정상 시계열 - 매칭 후 직전역에서 알람`() {
        val fixture = ReplayFixture(
            meta = ReplayMeta("테스트", "L", "normal"),
            trip = ReplayTrip("A", "D"),
            frames = listOf(
                // trainSttus: 1=도착. B 에서 출발(2) 이면 규칙 B 조기 알람이 먼저 걸려 C 전에 끝남.
                ReplayFrame(0, listOf(ReplayPosition("T1", "A", "A", "0", 1, 1_700_000_000_000L))),
                ReplayFrame(30, listOf(ReplayPosition("T1", "B", "B", "0", 1, 1_700_000_030_000L))),
                ReplayFrame(60, listOf(ReplayPosition("T1", "C", "C", "0", 1, 1_700_000_060_000L))),
            ),
            expect = ReplayExpect("ALERTED", "T1"),
        )
        val result = ReplayTripRunner().run(fixture, graph, polarity)
        assertTrue(result.triggered)
        assertEquals(TripState.ALERTED, result.finalSession.state)
        assertEquals("T1", result.finalSession.boardedTrainNo)
        assertEquals("C", result.finalSession.currentStatnId)
    }

    @Test
    fun `매칭 창 - 후보 2대면 MATCHING 유지 후 1대만 남으면 확정`() {
        val fixture = ReplayFixture(
            meta = ReplayMeta("테스트", "L"),
            trip = ReplayTrip("A", "D"),
            frames = listOf(
                ReplayFrame(
                    0,
                    listOf(
                        ReplayPosition("T1", "A", "A", "0", 1, 1000L),
                        ReplayPosition("T2", "A", "A", "0", 1, 1000L),
                    ),
                ),
                ReplayFrame(15, listOf(ReplayPosition("T1", "A", "A", "0", 1, 15000L))),
            ),
        )
        val result = ReplayTripRunner().run(fixture, graph, polarity)
        assertEquals(TripState.TRACKING, result.finalSession.state)
        assertEquals("T1", result.finalSession.boardedTrainNo)
    }

    @Test
    fun `열차 소실 후 가속도 추정으로 알람`() {
        val fixture = ReplayFixture(
            meta = ReplayMeta("테스트", "L"),
            trip = ReplayTrip("A", "D"),
            frames = listOf(
                ReplayFrame(0, listOf(ReplayPosition("T1", "A", "A", "0", 1, 1_700_000_000_000L))),
                ReplayFrame(20, listOf(ReplayPosition("T1", "B", "B", "0", 1, 1_700_000_020_000L))),
                // 소실 — accel 로 C 추정
                ReplayFrame(40, emptyList()),
            ),
        )
        val result = ReplayTripRunner().run(
            fixture, graph, polarity,
            accelEstimateAtFrame = { index, session ->
                if (index == 2 && session.boardedTrainNo != null) "C" else null
            },
        )
        assertTrue(result.triggered)
        assertEquals(TripState.ALERTED, result.finalSession.state)
        assertEquals("C", result.finalSession.currentStatnId)
    }
}
