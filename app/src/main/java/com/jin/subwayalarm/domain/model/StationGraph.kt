package com.jin.subwayalarm.domain.model

/**
 * 한 노선의 역 위상 그래프 (순수 로직, 단위테스트 대상).
 *
 * MVP(LINEAR)는 orderInLine + 진행 방향으로 다음/이전 역을 계산한다.
 * 직전역 판정(작업순서 6)·남은 역 수 계산의 토대.
 *
 * 설계 1-6: 배열 인덱스(indexOf±1)가 아니라 방향 그래프로 구현해
 * 2차의 LOOP/BRANCHED 확장 시 알고리즘을 갈아엎지 않는다.
 */
class StationGraph(stations: List<Station>) {

    private val ordered: List<Station> = stations.sortedBy { it.order }
    private val byId: Map<String, Station> = ordered.associateBy { it.statnId }
    private val byOrder: Map<Int, Station> = ordered.associateBy { it.order }

    init {
        require(ordered.isNotEmpty()) { "역 목록이 비어 있습니다." }
        require(byId.size == ordered.size) { "중복된 statnId 가 있습니다." }
        require(byOrder.size == ordered.size) { "중복된 orderInLine 이 있습니다." }
    }

    val size: Int get() = ordered.size
    val stations: List<Station> get() = ordered

    fun contains(statnId: String): Boolean = byId.containsKey(statnId)

    fun station(statnId: String): Station? = byId[statnId]

    private fun require(statnId: String): Station =
        byId[statnId] ?: throw IllegalArgumentException("그래프에 없는 역: $statnId")

    /**
     * 출발역 → 목적역의 진행 방향을 순번 비교로 도출.
     * 같은 역이면 예외.
     */
    fun directionBetween(originId: String, destId: String): Direction {
        val o = require(originId)
        val d = require(destId)
        require(o.order != d.order) { "출발역과 목적역이 동일합니다: $originId" }
        return if (d.order > o.order) Direction.ASCENDING else Direction.DESCENDING
    }

    /** 진행 방향 기준 current 의 다음 역 (없으면 null = 종착 너머). */
    fun nextStation(currentId: String, direction: Direction): Station? {
        val c = require(currentId)
        return byOrder[c.order + direction.step]
    }

    /** 진행 방향 기준 current 의 이전 역 (없으면 null = 시발 너머). */
    fun previousStation(currentId: String, direction: Direction): Station? =
        nextStation(currentId, direction.opposite)

    /**
     * 목적지로부터 [leadStations]개 전에 지나는 역(기본 1 = "한 역 전").
     * 도중에 그래프 경계를 벗어나면(시발역 인근) null.
     */
    fun stationBeforeArrival(destId: String, direction: Direction, leadStations: Int = 1): Station? {
        require(leadStations >= 1) { "leadStations 는 1 이상이어야 합니다: $leadStations" }
        var current = require(destId)
        repeat(leadStations) {
            current = previousStation(current.statnId, direction) ?: return null
        }
        return current
    }

    /**
     * current 에서 dest 까지 남은 정차역 수(현재역 제외, 목적지 포함 거리).
     * 같은 노선 내 단순 순번 차이의 절댓값.
     */
    fun remainingStops(currentId: String, destId: String): Int {
        val c = require(currentId)
        val d = require(destId)
        return kotlin.math.abs(d.order - c.order)
    }

    /** 출발/목적이 모두 존재하고 서로 다른지. */
    fun isValidTrip(originId: String, destId: String): Boolean =
        contains(originId) && contains(destId) && byId[originId]!!.order != byId[destId]!!.order

    /**
     * origin → dest 까지 진행 방향 순서대로의 역 목록(양끝 포함). 여정 시각화용.
     * 유효하지 않은 여정이면 빈 리스트.
     */
    fun stationsBetween(originId: String, destId: String): List<Station> {
        if (!isValidTrip(originId, destId)) return emptyList()
        val o = byId.getValue(originId)
        val d = byId.getValue(destId)
        val range = if (d.order >= o.order) o.order..d.order else o.order downTo d.order
        return range.mapNotNull { byOrder[it] }
    }
}
