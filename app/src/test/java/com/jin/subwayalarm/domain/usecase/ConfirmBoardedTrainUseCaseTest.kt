package com.jin.subwayalarm.domain.usecase

import com.jin.subwayalarm.domain.model.TrackingMode
import com.jin.subwayalarm.domain.model.TripSession
import com.jin.subwayalarm.domain.model.TripState
import org.junit.Assert.assertEquals
import org.junit.Test

class ConfirmBoardedTrainUseCaseTest {

    private val useCase = ConfirmBoardedTrainUseCase()

    @Test
    fun `수동 확정 시 TRACKING 과 trainNo 설정`() {
        val session = TripSession(
            originStatnId = "A", destStatnId = "D", lineId = "L",
            currentStatnId = "A", lastSyncAtEpochMillis = 0, state = TripState.MATCHING,
        )
        val updated = useCase(session, "T9", nowEpochMillis = 1000L, currentStatnId = "B")
        assertEquals("T9", updated.boardedTrainNo)
        assertEquals(TripState.TRACKING, updated.state)
        assertEquals(TrackingMode.API, updated.mode)
        assertEquals("B", updated.currentStatnId)
        assertEquals(1000L, updated.lastSyncAtEpochMillis)
    }
}
