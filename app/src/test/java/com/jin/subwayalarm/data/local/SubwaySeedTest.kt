package com.jin.subwayalarm.data.local

import com.jin.subwayalarm.domain.model.Direction
import com.jin.subwayalarm.domain.model.StationGraph
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SubwaySeedTest {

    private val stations = SubwaySeed.line7Stations

    @Test
    fun `시드는 12개 역, 순번 0부터 연속`() {
        assertEquals(12, stations.size)
        assertEquals((0..11).toList(), stations.map { it.order })
    }

    @Test
    fun `statnId 와 order 가 모두 유일해 그래프 구성 가능`() {
        val g = StationGraph(stations) // 중복 시 init 에서 예외
        assertEquals(12, g.size)
    }

    @Test
    fun `next prev 링크가 인접 순번과 일치`() {
        for (i in stations.indices) {
            val s = stations[i]
            val expectedNext = stations.getOrNull(i + 1)?.statnId
            val expectedPrev = stations.getOrNull(i - 1)?.statnId
            assertEquals("next@$i", expectedNext, s.nextStatnId)
            assertEquals("prev@$i", expectedPrev, s.prevStatnId)
        }
    }

    @Test
    fun `마지막 역만 avgSecToNext 가 0`() {
        stations.dropLast(1).forEach { assertTrue(it.avgSecToNext > 0) }
        assertEquals(0, stations.last().avgSecToNext)
    }

    @Test
    fun `시드 그래프에서 고속터미널→용마산 직전역은 중곡`() {
        val g = StationGraph(stations)
        val origin = stations.first { it.name == "고속터미널" }.statnId
        val dest = stations.first { it.name == "용마산" }.statnId
        val dir = g.directionBetween(origin, dest)
        assertEquals(Direction.ASCENDING, dir)
        assertEquals("중곡", g.stationBeforeArrival(dest, dir)?.name)
    }

    @Test
    fun `반대 방향 용마산→고속터미널 직전역은 반포`() {
        val g = StationGraph(stations)
        val origin = stations.first { it.name == "용마산" }.statnId
        val dest = stations.first { it.name == "고속터미널" }.statnId
        val dir = g.directionBetween(origin, dest)
        assertEquals(Direction.DESCENDING, dir)
        assertEquals("반포", g.stationBeforeArrival(dest, dir)?.name)
        // 고속터미널은 ASCENDING 기준 시발역 → DESCENDING 진행 시 직전역 존재(반포), OK
        assertNull(g.nextStation(dest, Direction.DESCENDING)) // 고속터미널 너머 없음
    }
}
