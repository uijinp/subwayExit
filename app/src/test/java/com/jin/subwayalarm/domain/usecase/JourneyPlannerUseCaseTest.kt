package com.jin.subwayalarm.domain.usecase

import com.jin.subwayalarm.data.local.SubwaySeed
import com.jin.subwayalarm.domain.model.SubwayNetwork
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JourneyPlannerUseCaseTest {

    private val useCase = JourneyPlannerUseCase()

    private fun realNetwork(): SubwayNetwork {
        val perLine = SubwaySeed.allLines.associate { it.meta.lineId to it.stations }
        val metas = SubwaySeed.allLines.map { it.meta }
        val edges = SubwaySeed.allLines.mapNotNull { s -> s.edges?.let { s.meta.lineId to it } }.toMap()
        return SubwayNetwork(perLine, metas, perLineEdges = edges)
    }

    @Test
    fun `같은 노선 내 이동은 환승 없이 1구간`() {
        val legs = useCase(realNetwork(), "장암", "석남(거북시장)") // 7호선 양끝
        assertEquals(1, legs?.size)
        assertEquals("1007", legs!![0].lineId)
        assertEquals("장암", legs[0].boardStationName)
        assertEquals("석남(거북시장)", legs[0].alightStationName)
    }

    @Test
    fun `인천에서 동작까지 경로가 잡힌다`() {
        // 인천은 1호선·수인분당 공용 역명 — 어느 노선으로 시작해도 동작(현충원)에 도착하면 된다.
        val legs = useCase(realNetwork(), "인천", "동작(현충원)")
        assertTrue("legs=$legs", legs != null && legs.isNotEmpty())
        assertEquals("인천", legs!!.first().boardStationName)
        assertEquals("동작(현충원)", legs.last().alightStationName)
        assertTrue("환승 과다 legs=$legs", legs.size <= 3)
    }

    @Test
    fun `역방향도 경로를 찾는다`() {
        val legs = useCase(realNetwork(), "동작(현충원)", "인천")
        assertTrue(legs != null && legs.isNotEmpty())
        assertEquals("동작(현충원)", legs!!.first().boardStationName)
        assertEquals("인천", legs.last().alightStationName)
        assertTrue(legs.size <= 3)
    }

    @Test
    fun `존재하지 않는 역명은 null`() {
        assertNull(useCase(realNetwork(), "없는역", "인천"))
        assertNull(useCase(realNetwork(), "인천", "없는역"))
    }

    @Test
    fun `출발과 목적지가 같으면 null`() {
        assertNull(useCase(realNetwork(), "인천", "인천"))
    }

    @Test
    fun `3호선 경유 환승 6호선行도 정상 계산된다`() {
        val legs = useCase(realNetwork(), "지축", "봉화산(서울의료원)")
        assertTrue("legs=$legs", legs != null && legs.isNotEmpty())
        assertEquals("봉화산(서울의료원)", legs!!.last().alightStationName)
        assertTrue(legs.size <= 3)
    }

    @Test
    fun `2호선 순환 구간 경로`() {
        val legs = useCase(realNetwork(), "강남", "홍대입구")
        assertTrue("legs=$legs", legs != null)
        assertEquals(1, legs!!.size)
        assertEquals("1002", legs[0].lineId)
    }
}
