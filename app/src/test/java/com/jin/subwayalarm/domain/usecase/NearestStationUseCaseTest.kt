package com.jin.subwayalarm.domain.usecase

import com.jin.subwayalarm.data.local.SubwaySeed
import com.jin.subwayalarm.domain.model.LatLon
import com.jin.subwayalarm.domain.model.Station
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NearestStationUseCaseTest {

    private val useCase = NearestStationUseCase()

    @Test
    fun `건대입구 실좌표 근처면 건대입구를 반환`() {
        // 건대입구역 좌표에서 아주 가까운 지점
        val near = LatLon(37.5407, 127.0711)
        val result = useCase(near, SubwaySeed.line7Stations)
        assertEquals("건대입구", result?.station?.name)
    }

    @Test
    fun `너무 먼 곳(부산)이면 null`() {
        val busan = LatLon(35.1796, 129.0756)
        assertNull(useCase(busan, SubwaySeed.line7Stations))
    }

    @Test
    fun `좌표 미설정 역은 후보에서 제외`() {
        val stations = listOf(
            Station("A", "L", "A", 0, 100, lat = 0.0, lon = 0.0), // 미설정
            Station("B", "L", "B", 1, 100, lat = 37.5, lon = 127.0),
        )
        val result = useCase(LatLon(37.5001, 127.0001), stations)
        assertEquals("B", result?.station?.name)
    }

    @Test
    fun `maxDistance 를 넘으면 null`() {
        val stations = listOf(Station("A", "L", "A", 0, 100, lat = 37.5, lon = 127.0))
        // 약 1.5km 떨어진 지점, 기본 maxDistance(1200m) 초과
        val far = LatLon(37.5135, 127.0)
        assertNull(useCase(far, stations))
    }
}
