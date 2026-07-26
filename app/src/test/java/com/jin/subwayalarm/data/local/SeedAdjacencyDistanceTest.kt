package com.jin.subwayalarm.data.local

import com.jin.subwayalarm.domain.model.Station
import com.jin.subwayalarm.domain.model.StationGraph
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

/**
 * 시드 정합성: 그래프상 "인접"한 두 역은 실제로도 가까워야 한다.
 *
 * order 나 edges 가 지리 순서와 어긋나면 노선이 도시를 가로질러 이어진 것처럼 되고,
 * 그 결과 (1) 환승 경로가 엉뚱하게 계산되고 (2) 추적 시 직전역·남은 역 수가 전부 틀린다.
 *
 * 실제 사고(2026-07-16 감사): 경의중앙선 56개 간선 중 44개, 수인분당선 51/62,
 * 공항철도 11/13, 신분당선 10/15 가 지리적으로 말이 안 되게 연결돼 있었다
 * (도농↔운천 45km, 서울역↔인천공항1터미널 47km 등). 역명·좌표는 정확했고 order/edges 만 뒤섞여 있었다.
 *
 * order 기준이 아니라 반드시 [StationGraph.adjacent] (명시 엣지 반영) 로 검사해야 한다 —
 * 2·5·8호선처럼 분기가 있어 명시 엣지를 쓰는 노선은 order 인접이 의미가 없기 때문.
 */
class SeedAdjacencyDistanceTest {

    /** 실제로 역간 거리가 먼 정상 구간 (광역급행·교외선). 역명 쌍은 순서 무관. */
    private val allowed: Set<Set<String>> = setOf(
        setOf("연천", "청산"), // 1호선 경원선 북부
        setOf("전곡", "소요산"),
        setOf("평택", "성환"), // 1호선 경부 남부
        setOf("부발", "세종대왕릉"), // 경강선
        setOf("아시아드경기장", "서해구청"), // 인천2호선
        setOf("청라국제도시", "영종"), // 공항철도 — 영종대교로 바다를 건너 중간역 없음
    )

    /** GTX 는 정차역 자체가 드물어 전 구간이 장거리 — 노선 단위로 제외. */
    private val allowedLines = setOf("1032") // GTX-A

    private fun meters(a: Station, b: Station): Double {
        val dLat = (a.lat - b.lat) * 111_000.0
        val dLon = (a.lon - b.lon) * 88_000.0
        return sqrt(dLat * dLat + dLon * dLon)
    }

    @Test
    fun `그래프상 인접한 역은 실제로도 8km 이내여야 한다`() {
        val offenders = mutableListOf<String>()

        for (line in SubwaySeed.allLines) {
            if (line.meta.lineId in allowedLines) continue
            val graph = StationGraph(line.stations, line.edges)
            val seen = mutableSetOf<String>()

            for (station in line.stations) {
                for (neighbor in graph.adjacent(station.statnId)) {
                    val key = listOf(station.statnId, neighbor.statnId).sorted().joinToString("-")
                    if (!seen.add(key)) continue
                    if (station.lat == 0.0 || neighbor.lat == 0.0) continue
                    if (setOf(station.name, neighbor.name) in allowed) continue

                    val distance = meters(station, neighbor)
                    if (distance > 8_000) {
                        offenders += "${line.meta.lineName}: ${station.name} ↔ ${neighbor.name} " +
                            "= ${(distance / 1000).toInt()}km"
                    }
                }
            }
        }

        assertTrue(
            "그래프상 인접한데 실제로는 먼 역 쌍 발견 — 해당 노선의 order/edges 가 지리 순서와 " +
                "어긋났을 가능성이 높다(HANDOFF §A 참고). 실제 장거리 구간이면 allowed 에 추가할 것:\n" +
                offenders.joinToString("\n"),
            offenders.isEmpty(),
        )
    }

    @Test
    fun `노선의 모든 역이 하나의 그래프로 연결돼 있어야 한다`() {
        val broken = mutableListOf<String>()
        for (line in SubwaySeed.allLines) {
            val graph = StationGraph(line.stations, line.edges)
            val start = line.stations.first().statnId
            val unreachable = line.stations.filter { graph.path(start, it.statnId) == null }
            if (unreachable.isNotEmpty()) {
                broken += "${line.meta.lineName}: ${unreachable.size}역 도달 불가 " +
                    unreachable.take(5).map { it.name }
            }
        }
        assertTrue("끊어진 노선 발견:\n" + broken.joinToString("\n"), broken.isEmpty())
    }
}
