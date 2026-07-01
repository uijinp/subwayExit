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
        return SubwayNetwork(perLine, metas)
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
    fun `직행이 없으면 노량진 환승으로 1호선에서 9호선으로 넘어간다`() {
        // 인천(1호선 전용) → 동작(현충원)(9호선 전용) : 노량진에서 1↔9 환승만 연결됨
        val legs = useCase(realNetwork(), "인천", "동작(현충원)")
        assertEquals(2, legs?.size)
        assertEquals("1001", legs!![0].lineId)
        assertEquals("인천", legs[0].boardStationName)
        assertEquals("노량진", legs[0].alightStationName)
        assertEquals("1009", legs[1].lineId)
        assertEquals("노량진", legs[1].boardStationName)
        assertEquals("동작(현충원)", legs[1].alightStationName)
    }

    @Test
    fun `역방향도 대칭적으로 경로를 찾는다`() {
        val legs = useCase(realNetwork(), "동작(현충원)", "인천")
        assertEquals(2, legs?.size)
        assertEquals("1009", legs!![0].lineId)
        assertEquals("동작(현충원)", legs[0].boardStationName)
        assertEquals("노량진", legs[0].alightStationName)
        assertEquals("1001", legs[1].lineId)
        assertEquals("노량진", legs[1].boardStationName)
        assertEquals("인천", legs[1].alightStationName)
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
    fun `3호선 경유 환승(불광 경유 6호선行)도 정상 계산된다`() {
        // 지축(3호선 전용) → 태릉입구(6호선, 7호선도 지남) : 3→6호선 환승은 불광/약수/연신내 중 하나
        val legs = useCase(realNetwork(), "지축", "봉화산(서울의료원)") // 6호선 전용역
        assertTrue("legs=$legs", (legs?.size ?: 0) in 1..2)
        assertEquals("1006", legs!!.last().lineId)
        assertEquals("봉화산(서울의료원)", legs.last().alightStationName)
        if (legs.size == 2) {
            assertEquals("1003", legs[0].lineId)
            assertTrue(legs[0].alightStationName in listOf("불광", "약수", "연신내"))
        }
    }
}
