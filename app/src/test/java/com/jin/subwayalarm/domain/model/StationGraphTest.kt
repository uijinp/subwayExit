package com.jin.subwayalarm.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StationGraphTest {

    // A(0) - B(1) - C(2) - D(3) - E(4)
    private fun line(): StationGraph {
        val names = listOf("A", "B", "C", "D", "E")
        val stations = names.mapIndexed { i, n ->
            Station(statnId = n, lineId = "L", name = n, order = i, avgSecToNext = 100)
        }
        return StationGraph(stations)
    }

    @Test
    fun `directionBetween 은 순번 비교로 방향을 도출`() {
        val g = line()
        assertEquals(Direction.ASCENDING, g.directionBetween("A", "E"))
        assertEquals(Direction.DESCENDING, g.directionBetween("E", "A"))
        assertEquals(Direction.ASCENDING, g.directionBetween("B", "C"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `directionBetween 동일역이면 예외`() {
        line().directionBetween("C", "C")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `그래프에 없는 역이면 예외`() {
        line().directionBetween("A", "Z")
    }

    @Test
    fun `nextStation 은 진행 방향 기준 다음 역`() {
        val g = line()
        assertEquals("C", g.nextStation("B", Direction.ASCENDING)?.statnId)
        assertEquals("A", g.nextStation("B", Direction.DESCENDING)?.statnId)
    }

    @Test
    fun `종착 너머의 다음 역은 null`() {
        val g = line()
        assertNull(g.nextStation("E", Direction.ASCENDING))
        assertNull(g.nextStation("A", Direction.DESCENDING))
    }

    @Test
    fun `stationBeforeArrival 은 한 역 전 (진행 방향 반영)`() {
        val g = line()
        // ASCENDING 으로 D 도착 직전 = C
        assertEquals("C", g.stationBeforeArrival("D", Direction.ASCENDING)?.statnId)
        // DESCENDING 으로 B 도착 직전 = C
        assertEquals("C", g.stationBeforeArrival("B", Direction.DESCENDING)?.statnId)
    }

    @Test
    fun `stationBeforeArrival 은 leadStations 만큼 추가로 거슬러 올라감`() {
        val g = line()
        // ASCENDING 으로 E 도착 기준 2역 전 = C
        assertEquals("C", g.stationBeforeArrival("E", Direction.ASCENDING, leadStations = 2)?.statnId)
    }

    @Test
    fun `leadStations 가 그래프 경계를 넘으면 null`() {
        val g = line()
        // ASCENDING 으로 B 도착 기준 3역 전은 그래프 밖(A 이전) → null
        assertNull(g.stationBeforeArrival("B", Direction.ASCENDING, leadStations = 3))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `leadStations 0 이하는 예외`() {
        line().stationBeforeArrival("E", Direction.ASCENDING, leadStations = 0)
    }

    @Test
    fun `진행 방향상 시발역의 직전역은 null`() {
        val g = line()
        // ASCENDING 인데 목적지가 최저 순번 A → 직전역 없음
        assertNull(g.stationBeforeArrival("A", Direction.ASCENDING))
        // DESCENDING 인데 목적지가 최고 순번 E → 직전역 없음
        assertNull(g.stationBeforeArrival("E", Direction.DESCENDING))
    }

    @Test
    fun `remainingStops 는 순번 차이의 절댓값`() {
        val g = line()
        assertEquals(3, g.remainingStops("A", "D"))
        assertEquals(3, g.remainingStops("D", "A"))
        assertEquals(0, g.remainingStops("C", "C"))
    }

    @Test
    fun `isValidTrip 검증`() {
        val g = line()
        assertTrue(g.isValidTrip("A", "E"))
        assertFalse(g.isValidTrip("C", "C"))
        assertFalse(g.isValidTrip("A", "Z"))
    }

    @Test
    fun `정렬되지 않은 입력도 order 로 정렬`() {
        val shuffled = listOf(
            Station("C", "L", "C", 2, 100),
            Station("A", "L", "A", 0, 100),
            Station("B", "L", "B", 1, 100),
        )
        val g = StationGraph(shuffled)
        assertEquals(listOf("A", "B", "C"), g.stations.map { it.statnId })
    }

    @Test(expected = IllegalArgumentException::class)
    fun `중복 order 는 예외`() {
        StationGraph(
            listOf(
                Station("A", "L", "A", 0, 100),
                Station("B", "L", "B", 0, 100),
            ),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `빈 목록은 예외`() {
        StationGraph(emptyList())
    }
}
