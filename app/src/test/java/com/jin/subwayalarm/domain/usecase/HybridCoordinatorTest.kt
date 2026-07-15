package com.jin.subwayalarm.domain.usecase

import com.jin.subwayalarm.domain.model.Direction
import com.jin.subwayalarm.domain.model.Station
import com.jin.subwayalarm.domain.model.StationGraph
import com.jin.subwayalarm.domain.model.TrackingMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HybridCoordinatorTest {

    private val graph = StationGraph(
        listOf("A", "B", "C", "D").mapIndexed { i, n -> Station(n, "L", n, i, 120) },
    )

    @Test
    fun `API 있으면 resync 하고 센서 pause`() {
        val r = HybridCoordinator.coordinate(
            HybridInput(graph, Direction.ASCENDING, "A", apiCorrectedStatnId = "B", accelStopCountSinceResync = 3),
        )
        assertEquals("B", r.estimatedStatnId)
        assertEquals(TrackingMode.API, r.mode)
        assertTrue(r.pauseSensor)
        assertTrue(r.shouldResyncCounter)
    }

    @Test
    fun `API 소실 시 정차 횟수만큼 전진`() {
        val r = HybridCoordinator.coordinate(
            HybridInput(graph, Direction.ASCENDING, "A", apiCorrectedStatnId = null, accelStopCountSinceResync = 2),
        )
        assertEquals("C", r.estimatedStatnId)
        assertEquals(TrackingMode.ACCEL, r.mode)
        assertEquals(2, r.stationsAdvancedByAccel)
        assertTrue(!r.pauseSensor)
    }
}
