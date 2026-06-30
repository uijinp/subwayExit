package com.jin.subwayalarm.domain.usecase

import com.jin.subwayalarm.domain.model.Direction
import com.jin.subwayalarm.domain.model.Station
import com.jin.subwayalarm.domain.model.StationGraph
import org.junit.Assert.assertEquals
import org.junit.Test

class RecptnDtCorrectorTest {

    // A(0) -100s- B(1) -100s- C(2) -100s- D(3, 종착, avgSecToNext=0)
    private fun graph(): StationGraph = StationGraph(
        listOf(
            Station("A", "L", "A", 0, 100),
            Station("B", "L", "B", 1, 100),
            Station("C", "L", "C", 2, 100),
            Station("D", "L", "D", 3, 0),
        ),
    )

    private fun correct(rawStatnId: String, lagSeconds: Long, direction: Direction = Direction.ASCENDING) =
        RecptnDtCorrector.correct(
            graph = graph(),
            rawStatnId = rawStatnId,
            direction = direction,
            recptnDtEpochMillis = 0L,
            nowEpochMillis = lagSeconds * 1000L,
        )

    @Test
    fun `lag 0 이면 보정 없음`() {
        val result = correct("A", lagSeconds = 0)
        assertEquals("A", result.statnId)
        assertEquals(0, result.stationsAdvanced)
    }

    @Test
    fun `세그먼트의 50 미만 lag 는 보정 없음`() {
        val result = correct("A", lagSeconds = 49) // 49/100 = 0.49 < 0.5
        assertEquals("A", result.statnId)
        assertEquals(0, result.stationsAdvanced)
    }

    @Test
    fun `세그먼트의 정확히 50 부터는 한 역 보정 (조기 알람 규칙)`() {
        val result = correct("A", lagSeconds = 50) // 50/100 = 0.5 → 경계, 조기 쪽
        assertEquals("B", result.statnId)
        assertEquals(1, result.stationsAdvanced)
    }

    @Test
    fun `여러 구간에 걸친 lag 는 누적 보정`() {
        // A→B(100)→C(100) 소진 후 남은 lag=60, 다음 구간(C→D,100)의 60% → D 까지 보정
        val result = correct("A", lagSeconds = 260)
        assertEquals("D", result.statnId)
        assertEquals(3, result.stationsAdvanced)
    }

    @Test
    fun `종착역(avgSecToNext 0)은 lag 가 커도 더 진행하지 않음`() {
        val result = correct("D", lagSeconds = 10_000)
        assertEquals("D", result.statnId)
        assertEquals(0, result.stationsAdvanced)
    }

    @Test
    fun `그래프 경계에 도달하면 더 보정하지 않고 멈춤`() {
        // C 에서 시작, lag 가 막대해도 D 까지만(종착) 가고 더는 못 감
        val result = correct("C", lagSeconds = 100_000)
        assertEquals("D", result.statnId)
        assertEquals(1, result.stationsAdvanced)
    }

    @Test
    fun `방향이 DESCENDING 이면 반대로 보정`() {
        val result = correct("D", lagSeconds = 50, direction = Direction.DESCENDING)
        assertEquals("C", result.statnId)
        assertEquals(1, result.stationsAdvanced)
    }

    @Test
    fun `그래프에 없는 역은 원본 그대로 반환`() {
        val result = correct("Z", lagSeconds = 1000)
        assertEquals("Z", result.statnId)
        assertEquals(0, result.stationsAdvanced)
    }

    @Test
    fun `lag 가 음수(시계 오차 등)면 보정 없음`() {
        val result = correct("A", lagSeconds = -10)
        assertEquals("A", result.statnId)
        assertEquals(0, result.stationsAdvanced)
    }
}
