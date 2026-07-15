package com.jin.subwayalarm.domain.model

import com.jin.subwayalarm.data.local.SubwaySeed
import com.jin.subwayalarm.domain.usecase.JourneyPlannerUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WalkTransferTest {

    private fun realNetwork(): SubwayNetwork {
        val perLine = SubwaySeed.allLines.associate { it.meta.lineId to it.stations }
        val metas = SubwaySeed.allLines.map { it.meta }
        return SubwayNetwork(perLine, metas)
    }

    @Test
    fun `이수 neighbors 에 4호선 총신대 환승이 포함된다`() {
        val net = realNetwork()
        val neighbors = net.neighbors("이수", "1007")
        val xfer = neighbors.filter { it.isTransfer }
        assertTrue(xfer.any { it.lineId == "1004" && it.stationName == "총신대입구(이수)" })
    }

    @Test
    fun `총신대입구 neighbors 에 7호선 이수 환승이 포함된다`() {
        val net = realNetwork()
        val neighbors = net.neighbors("총신대입구(이수)", "1004")
        assertTrue(neighbors.any { it.isTransfer && it.lineId == "1007" && it.stationName == "이수" })
    }

    @Test
    fun `내방-사당 은 이수 환승 2구간이 최단`() {
        val legs = JourneyPlannerUseCase()(realNetwork(), "내방", "사당")
        assertEquals(2, legs?.size)
        assertEquals("1007", legs!![0].lineId)
        assertEquals("내방", legs[0].boardStationName)
        assertEquals("이수", legs[0].alightStationName)
        assertEquals("1004", legs[1].lineId)
        assertEquals("총신대입구(이수)", legs[1].boardStationName)
        assertEquals("사당", legs[1].alightStationName)
    }
}
