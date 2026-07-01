package com.jin.subwayalarm.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JourneyPlanTest {

    private fun leg(n: String) = Leg(lineId = n, lineName = "${n}호선", boardStationName = "A$n", alightStationName = "B$n")

    @Test
    fun `1구간 여정은 마지막 구간이자 다음 구간 없음`() {
        val plan = JourneyPlan(legs = listOf(leg("1")))
        assertEquals(leg("1"), plan.currentLeg)
        assertTrue(plan.isLastLeg)
        assertFalse(plan.hasNextLeg)
        assertEquals(1, plan.totalLegs)
    }

    @Test
    fun `다구간 여정의 중간 인덱스는 다음 구간 있음`() {
        val plan = JourneyPlan(legs = listOf(leg("1"), leg("9")), currentLegIndex = 0)
        assertEquals(leg("1"), plan.currentLeg)
        assertFalse(plan.isLastLeg)
        assertTrue(plan.hasNextLeg)
    }

    @Test
    fun `다구간 여정의 마지막 인덱스는 다음 구간 없음`() {
        val plan = JourneyPlan(legs = listOf(leg("1"), leg("9")), currentLegIndex = 1)
        assertEquals(leg("9"), plan.currentLeg)
        assertTrue(plan.isLastLeg)
        assertFalse(plan.hasNextLeg)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `빈 legs 는 예외`() {
        JourneyPlan(legs = emptyList())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `currentLegIndex 범위 초과는 예외`() {
        JourneyPlan(legs = listOf(leg("1")), currentLegIndex = 1)
    }
}
