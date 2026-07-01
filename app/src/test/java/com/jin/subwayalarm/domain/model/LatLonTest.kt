package com.jin.subwayalarm.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LatLonTest {

    @Test
    fun `같은 지점은 거리 0`() {
        val p = LatLon(37.5, 127.0)
        assertEquals(0.0, p.distanceMetersTo(p), 0.001)
    }

    @Test
    fun `건대입구-뚝섬유원지 실좌표 거리 약 1km(오차 100m 이내)`() {
        // 실제 두 역 사이 직선거리는 약 1.05km
        val konkuk = LatLon(37.540786, 127.071011)
        val ttukseom = LatLon(37.53154, 127.066704)
        val d = konkuk.distanceMetersTo(ttukseom)
        assertTrue("거리=$d", d in 900.0..1200.0)
    }

    @Test
    fun `거리는 대칭적`() {
        val a = LatLon(37.5, 127.0)
        val b = LatLon(37.6, 127.1)
        assertEquals(a.distanceMetersTo(b), b.distanceMetersTo(a), 0.001)
    }

    @Test
    fun `isUnset 판정`() {
        assertTrue(LatLon(0.0, 0.0).isUnset)
        assertFalse(LatLon(37.5, 127.0).isUnset)
    }
}
