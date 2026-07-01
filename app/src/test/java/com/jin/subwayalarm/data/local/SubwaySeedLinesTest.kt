package com.jin.subwayalarm.data.local

import com.jin.subwayalarm.domain.model.StationGraph
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SubwaySeedLinesTest {

    @Test
    fun `모든 노선이 유효한 그래프를 구성하고 좌표·극성을 가진다`() {
        SubwaySeed.allLines.forEach { seed ->
            val g = StationGraph(seed.stations) // 중복 statnId·order 면 여기서 예외
            assertTrue("${seed.meta.lineName} 역 없음", g.size > 0)
            assertNotNull("${seed.meta.lineName} 극성 미설정", seed.meta.directionPolarity)
            seed.stations.forEach {
                assertTrue("${it.name} 좌표 미설정", !it.latLon.isUnset)
                assertEquals("statnId 접두어", seed.meta.lineId, it.statnId.substring(0, 4))
            }
        }
    }

    @Test
    fun `노선 ID 는 서로 겹치지 않는다`() {
        val ids = SubwaySeed.allLines.map { it.meta.lineId }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `3호선은 지축-오금 34역, statnId 는 base 319`() {
        val g = StationGraph(SubwaySeed.line3.stations)
        assertEquals(34, g.size)
        assertEquals("지축", g.stations.first().name)
        assertEquals("오금", g.stations.last().name)
        assertEquals("1003000319", g.stations.first().statnId)
    }

    @Test
    fun `4호선은 불암산-남태령 26역, statnId 는 base 409`() {
        val g = StationGraph(SubwaySeed.line4.stations)
        assertEquals(26, g.size)
        assertEquals("불암산", g.stations.first().name)
        assertEquals("남태령", g.stations.last().name)
        assertEquals("1004000409", g.stations.first().statnId)
    }

    @Test
    fun `6호선은 응암-신내 39역, statnId 는 base 610`() {
        val g = StationGraph(SubwaySeed.line6.stations)
        assertEquals(39, g.size)
        assertEquals("응암", g.stations.first().name)
        assertEquals("신내", g.stations.last().name)
        assertEquals("1006000610", g.stations.first().statnId)
    }

    @Test
    fun `9호선은 개화-중앙보훈병원 38역(연장 병합), statnId 는 base 901`() {
        val g = StationGraph(SubwaySeed.line9.stations)
        assertEquals(38, g.size)
        assertEquals("개화", g.stations.first().name)
        assertEquals("중앙보훈병원", g.stations.last().name)
        assertEquals("1009000901", g.stations.first().statnId)
    }

    @Test
    fun `1호선(경인)은 청량리-인천 38역, statnId 는 base 124`() {
        val g = StationGraph(SubwaySeed.line1Gyeongin.stations)
        assertEquals(38, g.size)
        assertEquals("청량리(서울시립대입구)", g.stations.first().name)
        assertEquals("인천", g.stations.last().name)
        assertEquals("1001000124", g.stations.first().statnId)
        assertEquals("1001000161", g.stations.last().statnId) // 인천 실측 statnId
    }

    @Test
    fun `현재 6개 노선(1·3·4·6·7·9호선) 수록`() {
        val ids = SubwaySeed.allLines.map { it.meta.lineId }.toSet()
        assertEquals(setOf("1001", "1003", "1004", "1006", "1007", "1009"), ids)
    }
}
