package com.jin.subwayalarm.domain.usecase

import com.jin.subwayalarm.domain.model.Station
import com.jin.subwayalarm.domain.model.StationGraph
import com.jin.subwayalarm.domain.model.TrainStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EvaluateAlertUseCaseTest {

    private val useCase = EvaluateAlertUseCase()

    // A(0) - B(1) - C(2) - D(3) - E(4)
    private fun graph(): StationGraph {
        val names = listOf("A", "B", "C", "D", "E")
        return StationGraph(
            names.mapIndexed { i, n -> Station(n, "L", n, i, 100) },
        )
    }

    @Test
    fun `규칙 A - 직전역 진입(0) 또는 도착(1)이면 트리거`() {
        val g = graph()
        // origin A → dest D, 진행 ASCENDING, 직전역 = C
        for (status in listOf(TrainStatus.APPROACHING, TrainStatus.ARRIVED)) {
            val result = useCase(g, originId = "A", destId = "D", currentStatnId = "C", trainStatus = status)
            assertTrue(result is AlertEvaluation.Trigger)
            result as AlertEvaluation.Trigger
            assertEquals("C", result.alertStationId)
            assertEquals(AlertEvaluation.Rule.AT_OR_ARRIVING_ALERT_STATION, result.matchedRule)
        }
    }

    @Test
    fun `규칙 B - 직전역 한 정거장 전에서 출발(2)이면 보수적 조기 트리거`() {
        val g = graph()
        // 직전역=C, 그 한 정거장 전=B, 거기서 출발(2)
        val result = useCase(g, originId = "A", destId = "D", currentStatnId = "B", trainStatus = TrainStatus.DEPARTED)
        assertTrue(result is AlertEvaluation.Trigger)
        result as AlertEvaluation.Trigger
        assertEquals("C", result.alertStationId)
        assertEquals(AlertEvaluation.Rule.DEPARTED_STATION_BEFORE_ALERT_STATION, result.matchedRule)
    }

    @Test
    fun `직전역에서 출발(2) 상태로 목격되면 만회 규칙(C)으로 트리거`() {
        // 직전역에서 이미 출발했다면 다음 정차역이 목적지 — 지금 안 울리면 기회가 없다.
        val g = graph()
        val result = useCase(g, originId = "A", destId = "D", currentStatnId = "C", trainStatus = TrainStatus.DEPARTED)
        assertTrue(result is AlertEvaluation.Trigger)
        result as AlertEvaluation.Trigger
        assertEquals(AlertEvaluation.Rule.AT_OR_PAST_ALERT_STATION, result.matchedRule)
    }

    @Test
    fun `만회 규칙 - 폴링 공백으로 목적지에서 처음 목격돼도 트리거`() {
        val g = graph()
        // 직전역 C 를 못 보고 건너뛰어 목적지 D 에서 처음 목격(백오프·보정 점프 상황).
        val result = useCase(g, originId = "A", destId = "D", currentStatnId = "D", trainStatus = TrainStatus.ARRIVED)
        assertTrue(result is AlertEvaluation.Trigger)
        result as AlertEvaluation.Trigger
        assertEquals(AlertEvaluation.Rule.AT_OR_PAST_ALERT_STATION, result.matchedRule)
    }

    @Test
    fun `만회 규칙 - 목적지를 지나쳐 목격돼도 트리거`() {
        val g = graph()
        val result = useCase(g, originId = "A", destId = "D", currentStatnId = "E", trainStatus = TrainStatus.DEPARTED)
        assertTrue(result is AlertEvaluation.Trigger)
    }

    @Test
    fun `만회 규칙 - 역방향 여정도 대칭 동작`() {
        val g = graph()
        // origin E → dest B(DESCENDING), 직전역 C 를 건너뛰고 A(목적지 너머)에서 목격.
        val result = useCase(g, originId = "E", destId = "B", currentStatnId = "A", trainStatus = TrainStatus.ARRIVED)
        assertTrue(result is AlertEvaluation.Trigger)
    }

    @Test
    fun `1정거장 여정 - 출발역에서 출발(2)이면 만회 규칙으로 트리거`() {
        val g = graph()
        // origin A → dest B: 직전역 = A(출발역). 출발 상태면 다음 역이 목적지.
        val result = useCase(g, originId = "A", destId = "B", currentStatnId = "A", trainStatus = TrainStatus.DEPARTED)
        assertTrue(result is AlertEvaluation.Trigger)
    }

    @Test
    fun `직전역의 한 정거장 전인데 도착(1) 상태면 트리거 아님(규칙 불일치)`() {
        val g = graph()
        val result = useCase(g, originId = "A", destId = "D", currentStatnId = "B", trainStatus = TrainStatus.ARRIVED)
        assertEquals(AlertEvaluation.NoTrigger, result)
    }

    @Test
    fun `목적지와 무관한 역이면 트리거 아님`() {
        val g = graph()
        val result = useCase(g, originId = "A", destId = "D", currentStatnId = "A", trainStatus = TrainStatus.APPROACHING)
        assertEquals(AlertEvaluation.NoTrigger, result)
    }

    @Test
    fun `alreadyAlerted 면 조건 충족해도 NoTrigger (중복 발화 방지)`() {
        val g = graph()
        val result = useCase(
            g,
            originId = "A",
            destId = "D",
            currentStatnId = "C",
            trainStatus = TrainStatus.ARRIVED,
            alreadyAlerted = true,
        )
        assertEquals(AlertEvaluation.NoTrigger, result)
    }

    @Test
    fun `출발역과 목적역이 같으면 Invalid`() {
        val g = graph()
        val result = useCase(g, originId = "C", destId = "C", currentStatnId = "C", trainStatus = TrainStatus.ARRIVED)
        assertTrue(result is AlertEvaluation.Invalid)
    }

    @Test
    fun `현재역이 그래프에 없으면 Invalid`() {
        val g = graph()
        val result = useCase(g, originId = "A", destId = "D", currentStatnId = "Z", trainStatus = TrainStatus.ARRIVED)
        assertTrue(result is AlertEvaluation.Invalid)
    }

    @Test
    fun `반대 방향 여정도 대칭적으로 동작`() {
        val g = graph()
        // origin E → dest B, 진행 DESCENDING, 직전역 = C
        val result = useCase(g, originId = "E", destId = "B", currentStatnId = "C", trainStatus = TrainStatus.APPROACHING)
        assertTrue(result is AlertEvaluation.Trigger)
        result as AlertEvaluation.Trigger
        assertEquals("C", result.alertStationId)
    }

    @Test
    fun `leadStations 2 면 두 역 전에서 트리거`() {
        val g = graph()
        // origin A → dest E, leadStations=2 → 직전역=C
        val result = useCase(
            g,
            originId = "A",
            destId = "E",
            currentStatnId = "C",
            trainStatus = TrainStatus.ARRIVED,
            leadStations = 2,
        )
        assertTrue(result is AlertEvaluation.Trigger)
        result as AlertEvaluation.Trigger
        assertEquals("C", result.alertStationId)
    }

    @Test
    fun `목적지가 진행방향상 시발 인근이라 직전역이 없으면 Invalid`() {
        val g = graph()
        // origin A → dest B(바로 다음역), leadStations=2 → A 이전(그래프 밖) → Invalid
        val result = useCase(
            g,
            originId = "A",
            destId = "B",
            currentStatnId = "A",
            trainStatus = TrainStatus.ARRIVED,
            leadStations = 2,
        )
        assertTrue(result is AlertEvaluation.Invalid)
    }
}
