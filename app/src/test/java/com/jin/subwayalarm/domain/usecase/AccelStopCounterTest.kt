package com.jin.subwayalarm.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccelStopCounterTest {

    @Test
    fun `정지 임계 지속 시 정차 1회 커밋`() {
        val counter = AccelStopCounter(
            moveThreshold = 0.8f,
            stopThreshold = 0.35f,
            stopDurationMs = 1000L,
            moveDurationMs = 200L,
        )
        // 이동 중
        assertFalse(counter.onSample(1.2f, 0))
        // 감속·정지
        assertFalse(counter.onSample(0.1f, 100))
        assertFalse(counter.onSample(0.1f, 500))
        assertTrue(counter.onSample(0.1f, 1200))
        assertEquals(1, counter.stopCount)
        assertEquals(AccelStopCounter.Phase.STOPPED, counter.phase)
    }

    @Test
    fun `resync 하면 카운터와 상태 리셋`() {
        val counter = AccelStopCounter(stopDurationMs = 100)
        counter.onSample(0.1f, 0)
        counter.onSample(0.1f, 200)
        assertEquals(1, counter.stopCount)
        counter.resync()
        assertEquals(0, counter.stopCount)
        assertEquals(AccelStopCounter.Phase.MOVING, counter.phase)
    }

    @Test
    fun `짧은 정지는 카운트하지 않음`() {
        val counter = AccelStopCounter(stopDurationMs = 5000)
        counter.onSample(1.0f, 0)
        counter.onSample(0.1f, 100)
        counter.onSample(1.0f, 200) // 복귀
        assertEquals(0, counter.stopCount)
    }
}
