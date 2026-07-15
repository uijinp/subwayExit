package com.jin.subwayalarm.domain.model

import com.jin.subwayalarm.data.local.SubwaySeed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SubwayNetworkTest {

    private fun realNetwork(): SubwayNetwork {
        val perLine = SubwaySeed.allLines.associate { it.meta.lineId to it.stations }
        val metas = SubwaySeed.allLines.map { it.meta }
        val edges = SubwaySeed.allLines.mapNotNull { seed ->
            seed.edges?.let { seed.meta.lineId to it }
        }.toMap()
        return SubwayNetwork(perLine, metas, perLineEdges = edges)
    }

    @Test
    fun `고속터미널은 3개 노선(3·7·9)을 지난다`() {
        val net = realNetwork()
        assertEquals(setOf("1003", "1007", "1009"), net.linesServing("고속터미널"))
    }

    @Test
    fun `실측 확인된 15개 환승역이 전부 2개 이상 노선에 걸쳐있다`() {
        val net = realNetwork()
        val transferStations = listOf(
            "고속터미널", "노량진", "노원", "동대문", "동묘앞", "동작(현충원)",
            "불광", "삼각지", "서울역", "약수", "연신내", "온수(성공회대입구)",
            "종로3가", "충무로", "태릉입구",
        )
        transferStations.forEach {
            assertTrue("$it 은 환승역이어야 함", net.linesServing(it).size >= 2)
        }
    }

    @Test
    fun `비환승역은 노선 1개만 반환`() {
        val net = realNetwork()
        assertEquals(setOf("1007"), net.linesServing("용마산")) // 7호선 전용역
    }

    @Test
    fun `존재하지 않는 역명은 빈 집합`() {
        assertEquals(emptySet<String>(), realNetwork().linesServing("없는역"))
    }

    @Test
    fun `neighbors 는 같은 노선 인접역 + 환승 노선을 함께 반환`() {
        val net = realNetwork()
        val neighbors = net.neighbors("고속터미널", "1007")
        // 같은 노선(7호선) 인접역 2개(반포·논현 방향) + 환승(3호선, 9호선) 2개
        val transferNeighbors = neighbors.filter { it.isTransfer }
        assertEquals(setOf("1003", "1009"), transferNeighbors.map { it.lineId }.toSet())
        transferNeighbors.forEach { assertEquals("고속터미널", it.stationName) }
        assertTrue(neighbors.any { !it.isTransfer })
    }

    @Test
    fun `stationOn 은 해당 노선의 실제 Station 을 반환`() {
        val net = realNetwork()
        val station = net.stationOn("1007", "고속터미널")
        assertEquals("고속터미널", station?.name)
        assertNull(net.stationOn("1007", "없는역"))
        assertNull(net.stationOn("없는노선", "고속터미널"))
    }
}
