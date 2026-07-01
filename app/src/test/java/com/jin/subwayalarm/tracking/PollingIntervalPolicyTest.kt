package com.jin.subwayalarm.tracking

import org.junit.Assert.assertEquals
import org.junit.Test

class PollingIntervalPolicyTest {

    @Test
    fun `4역 초과면 75초`() {
        assertEquals(75L, PollingIntervalPolicy.intervalSeconds(5))
        assertEquals(75L, PollingIntervalPolicy.intervalSeconds(100))
    }

    @Test
    fun `2~4역이면 30초`() {
        assertEquals(30L, PollingIntervalPolicy.intervalSeconds(2))
        assertEquals(30L, PollingIntervalPolicy.intervalSeconds(3))
        assertEquals(30L, PollingIntervalPolicy.intervalSeconds(4))
    }

    @Test
    fun `2역 미만이면 15초`() {
        assertEquals(15L, PollingIntervalPolicy.intervalSeconds(1))
        assertEquals(15L, PollingIntervalPolicy.intervalSeconds(0))
    }

    @Test
    fun `실패 0회면 백오프 없음`() {
        assertEquals(0L, PollingIntervalPolicy.backoffSeconds(0))
    }

    @Test
    fun `연속 실패마다 지수적으로 증가`() {
        assertEquals(15L, PollingIntervalPolicy.backoffSeconds(1))
        assertEquals(30L, PollingIntervalPolicy.backoffSeconds(2))
        assertEquals(60L, PollingIntervalPolicy.backoffSeconds(3))
        assertEquals(120L, PollingIntervalPolicy.backoffSeconds(4))
    }

    @Test
    fun `백오프는 600초를 넘지 않음`() {
        assertEquals(480L, PollingIntervalPolicy.backoffSeconds(6))
        assertEquals(600L, PollingIntervalPolicy.backoffSeconds(7))
        assertEquals(600L, PollingIntervalPolicy.backoffSeconds(100))
    }
}
