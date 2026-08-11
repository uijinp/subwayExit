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
            val g = StationGraph(seed.stations, seed.edges) // 중복 statnId·order 면 여기서 예외
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
    fun `1호선은 연천-인천-경부 포함 100역 이상 BRANCHED`() {
        val seed = SubwaySeed.line1
        assertTrue(seed.stations.size >= 100)
        assertEquals(com.jin.subwayalarm.domain.model.LineTopology.BRANCHED, seed.meta.topology)
        assertNotNull(seed.edges)
        val g = StationGraph(seed.stations, seed.edges)
        val names = seed.stations.map { it.name }.toSet()
        assertTrue(names.contains("연천"))
        assertTrue(names.contains("인천"))
        assertTrue(names.contains("수원"))
        assertTrue(names.contains("신창"))
        assertTrue(names.contains("광명") || names.any { it.contains("광명") })
        assertTrue(names.contains("서동탄"))
        // 구로 분기로 경인·경부 둘 다 도달
        assertTrue(g.path(
            seed.stations.first { it.name == "연천" }.statnId,
            seed.stations.first { it.name == "인천" }.statnId,
        ) != null)
        assertTrue(g.path(
            seed.stations.first { it.name.contains("청량리") }.statnId,
            seed.stations.first { it.name == "수원" }.statnId,
        ) != null)
        assertEquals("1001000161", seed.stations.first { it.name == "인천" }.statnId)
    }

    /**
     * 예전엔 1호선 시드가 경인 구간만 담아서 displayName 에 "(연천–인천·경부)" 로 범위를
     * 고지했고, 그걸 검증하는 테스트가 있었다. 지금은 전 구간(102역)이 들어와 고지가 불필요하고
     * (커버리지는 바로 위 `1호선은 연천-인천-경부 포함 100역 이상 BRANCHED` 가 검증한다),
     * 오히려 이름이 길면 역 검색 목록의 모든 행이 두 줄로 접혀 훑어보기 어려워진다.
     * 그래서 "범위 고지" 대신 "목록에서 읽기 좋은 길이" 를 불변식으로 둔다.
     */
    @Test
    fun `노선 표시명은 검색 목록에서 접히지 않게 짧아야 한다`() {
        val tooLong = SubwaySeed.allLines
            .map { it.meta.displayName }
            .filter { it.length > 10 }
        assertTrue(
            "표시명이 길면 '역명 · 노선A·노선B' 조합이 두 줄로 접힌다: $tooLong",
            tooLong.isEmpty(),
        )
    }

    @Test
    fun `1-9호선과 광역·경전철이 수록된다`() {
        val ids = SubwaySeed.allLines.map { it.meta.lineId }.toSet()
        assertTrue(ids.containsAll(setOf("1001", "1002", "1003", "1004", "1005", "1006", "1007", "1008", "1009")))
        assertTrue(ids.containsAll(setOf("1063", "1075", "1077", "1065", "1067", "1092", "1094")))
        // 이어서 추가: 서해·경강·GTX·인천·김포·의정부·용인
        assertTrue(ids.containsAll(setOf("1093", "1081", "1032", "3110", "3200", "4920", "4600", "4500")))
        assertTrue(SubwaySeed.allLines.size >= 24)
    }

    @Test
    fun `인천1호선 등은 경로용이며 실시간 미지원 플래그`() {
        val incheon = MoreLinesSeed.seed_3110
        assertTrue(!incheon.meta.supportsRealtime)
        assertTrue(MoreLinesSeed.seed_1093.meta.supportsRealtime) // 서해선은 실시간
    }

    @Test
    fun `2호선은 순환 엣지로 시청-충정로가 연결된다`() {
        val seed = ExtraLinesSeed.seed_1002
        val g = StationGraph(seed.stations, seed.edges)
        val cityhall = seed.stations.first { it.name == "시청" }
        val chungjeong = seed.stations.first { it.name.contains("충정로") }
        assertNotNull(g.path(cityhall.statnId, chungjeong.statnId))
        // 순환: 짧은 경로가 존재
        assertTrue(g.remainingStops(cityhall.statnId, chungjeong.statnId) < 20)
    }
}
