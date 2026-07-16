package com.jin.subwayalarm.data.local

import com.jin.subwayalarm.domain.model.DirectionPolarity
import com.jin.subwayalarm.domain.model.LineMeta
import com.jin.subwayalarm.domain.model.LineTopology
import com.jin.subwayalarm.domain.model.Station
import com.jin.subwayalarm.domain.model.SubwayNetwork
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

/**
 * 시드 데이터 정합성: SubwayNetwork 가 "역명"으로 노선 간 환승을 잇기 때문에,
 * 같은 이름의 역이 서로 다른 노선에 있으면 반드시 같은 물리 역(좌표 600m 이내)이어야 한다.
 *
 * 실제 사고 사례(2026-07-16 감사에서 적발):
 *  - 신촌: 2호선 ↔ 경의중앙선, 약 714m 떨어진 별개 역 → 가짜 환승
 *  - 양평: 5호선(영등포구) ↔ 경의중앙선(경기 양평군), 약 53km → 가짜 환승
 *  - 능곡: 진짜 환승역인데 서해선 좌표 오입력(27km 오차)으로 감사에 걸림
 * 앞으로 노선 시드를 추가할 때 이 테스트가 같은 유형의 사고를 막는다.
 */
class SeedStationNameCollisionTest {

    private fun distanceMeters(a: Station, b: Station): Double {
        val dLat = (a.lat - b.lat) * 111_000.0
        val dLon = (a.lon - b.lon) * 88_000.0
        return sqrt(dLat * dLat + dLon * dLon)
    }

    @Test
    fun `같은 이름의 다른 노선 역은 전부 좌표 600m 이내여야 한다`() {
        val all = SubwaySeed.allLines.flatMap { it.stations }
        val offenders = mutableListOf<String>()
        for ((name, group) in all.groupBy { it.name }) {
            for (a in group) {
                for (b in group) {
                    if (a.lineId >= b.lineId) continue
                    if (a.lat == 0.0 || b.lat == 0.0) continue
                    val dist = distanceMeters(a, b)
                    if (dist > 600) offenders += "$name: ${a.lineId} ↔ ${b.lineId} ${dist.toInt()}m"
                }
            }
        }
        assertTrue(
            "동명이역 가짜 환승 후보 발견 — 시드 역명을 구분 표기하거나 좌표를 정정할 것:\n" +
                offenders.joinToString("\n"),
            offenders.isEmpty(),
        )
    }

    @Test
    fun `SubwayNetwork 는 같은 이름이라도 멀리 떨어진 역을 환승으로 잇지 않는다`() {
        fun meta(id: String, name: String) = LineMeta(
            lineId = id, lineName = name, supportsRealtime = true,
            topology = LineTopology.LINEAR, directionPolarity = DirectionPolarity("1"),
        )
        // L1 의 "중앙" 과 L2 의 "중앙" 은 60km 떨어진 동명이역.
        val lineA = listOf(
            Station("A1", "L1", "시작", 0, 100, lat = 37.50, lon = 127.00),
            Station("A2", "L1", "중앙", 1, 100, lat = 37.51, lon = 127.00),
        )
        val lineB = listOf(
            Station("B1", "L2", "중앙", 0, 100, lat = 37.51, lon = 127.68),
            Station("B2", "L2", "끝", 1, 100, lat = 37.52, lon = 127.68),
        )
        val network = SubwayNetwork(
            perLineStations = mapOf("L1" to lineA, "L2" to lineB),
            lineMetas = listOf(meta("L1", "가상1호선"), meta("L2", "가상2호선")),
            walkTransfers = emptyMap(),
        )
        val transfers = network.neighbors("중앙", "L1").filter { it.isTransfer }
        assertTrue("멀리 떨어진 동명이역이 환승으로 연결됨: $transfers", transfers.isEmpty())
    }

    @Test
    fun `SubwayNetwork 는 좌표가 가까운 동명역은 정상적으로 환승 연결한다`() {
        fun meta(id: String, name: String) = LineMeta(
            lineId = id, lineName = name, supportsRealtime = true,
            topology = LineTopology.LINEAR, directionPolarity = DirectionPolarity("1"),
        )
        val lineA = listOf(
            Station("A1", "L1", "시작", 0, 100, lat = 37.50, lon = 127.00),
            Station("A2", "L1", "환승역", 1, 100, lat = 37.51, lon = 127.00),
        )
        val lineB = listOf(
            Station("B1", "L2", "환승역", 0, 100, lat = 37.5105, lon = 127.0005),
            Station("B2", "L2", "끝", 1, 100, lat = 37.52, lon = 127.00),
        )
        val network = SubwayNetwork(
            perLineStations = mapOf("L1" to lineA, "L2" to lineB),
            lineMetas = listOf(meta("L1", "가상1호선"), meta("L2", "가상2호선")),
            walkTransfers = emptyMap(),
        )
        val transfers = network.neighbors("환승역", "L1").filter { it.isTransfer }
        assertTrue("가까운 동명역 환승이 사라짐", transfers.any { it.lineId == "L2" })
    }
}
