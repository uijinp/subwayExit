package com.jin.subwayalarm.data.local

import com.jin.subwayalarm.domain.model.Direction
import com.jin.subwayalarm.domain.model.DirectionPolarity
import com.jin.subwayalarm.domain.model.StationGraph
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SubwaySeedTest {

    private val stations = SubwaySeed.line7Stations

    @Test
    fun `시드는 53개 역, 순번 0부터 연속(장암~석남)`() {
        assertEquals(53, stations.size)
        assertEquals((0..52).toList(), stations.map { it.order })
        assertEquals("장암", stations.first().name)
        assertEquals("석남(거북시장)", stations.last().name)
    }

    @Test
    fun `statnId 와 order 가 모두 유일해 그래프 구성 가능`() {
        val g = StationGraph(stations) // 중복 시 init 에서 예외
        assertEquals(53, g.size)
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
    fun `마지막 역(석남)만 avgSecToNext 가 0`() {
        stations.dropLast(1).forEach { assertTrue(it.avgSecToNext > 0) }
        assertEquals(0, stations.last().avgSecToNext)
    }

    @Test
    fun `실측 확인된 statnId 가 공식과 일치(작업3, 2026-07-01)`() {
        val byName = stations.associateBy { it.name }
        assertEquals("1007000709", byName.getValue("장암").statnId)
        assertEquals("1007000710", byName.getValue("도봉산").statnId)
        assertEquals("1007000719", byName.getValue("중화").statnId)
        assertEquals("1007000723", byName.getValue("용마산").statnId)
        assertEquals("1007000727", byName.getValue("건대입구").statnId)
        assertEquals("1007000734", byName.getValue("고속터미널").statnId)
        assertEquals("1007000750", byName.getValue("온수(성공회대입구)").statnId)
        assertEquals("1007000761", byName.getValue("석남(거북시장)").statnId)
    }

    @Test
    fun `directionPolarity 는 실측 확정값(ascendingUpdnLine=1, 석남 방향)`() {
        assertEquals(DirectionPolarity(ascendingUpdnLine = "1"), SubwaySeed.line7Meta.directionPolarity)
    }

    @Test
    fun `시드 그래프에서 고속터미널→용마산 직전역은 중곡`() {
        val g = StationGraph(stations)
        val origin = stations.first { it.name == "고속터미널" }.statnId
        val dest = stations.first { it.name == "용마산" }.statnId
        val dir = g.directionBetween(origin, dest)
        assertEquals(Direction.DESCENDING, dir) // 장암 방향(순번 감소)
        assertEquals("중곡", g.stationBeforeArrival(dest, dir)?.name)
    }

    @Test
    fun `반대 방향 용마산→고속터미널 직전역은 반포`() {
        val g = StationGraph(stations)
        val origin = stations.first { it.name == "용마산" }.statnId
        val dest = stations.first { it.name == "고속터미널" }.statnId
        val dir = g.directionBetween(origin, dest)
        assertEquals(Direction.ASCENDING, dir) // 석남 방향(순번 증가)
        assertEquals("반포", g.stationBeforeArrival(dest, dir)?.name)
    }

    @Test
    fun `실제 종착역(장암 석남) 너머로는 진행 불가`() {
        val g = StationGraph(stations)
        val jangam = stations.first { it.name == "장암" }.statnId
        val seoknam = stations.first { it.name == "석남(거북시장)" }.statnId
        assertNull(g.nextStation(jangam, Direction.DESCENDING))
        assertNull(g.nextStation(seoknam, Direction.ASCENDING))
    }
}
