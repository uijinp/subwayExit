package com.jin.subwayalarm.domain.model

/**
 * 한 노선의 역 위상 그래프 (순수 로직).
 *
 * - 기본: order 인접으로 LINEAR 엣지 생성
 * - [explicitEdges] 가 있으면 해당 무방향 엣지 사용 (1호선 BRANCHED 등)
 *
 * 직전역·남은 역·보정 진행은 모두 origin→dest 최단 경로 위에서 계산한다.
 */
class StationGraph(
    stations: List<Station>,
    explicitEdges: List<Pair<String, String>>? = null,
) {

    private val ordered: List<Station> = stations.sortedBy { it.order }
    private val byId: Map<String, Station> = ordered.associateBy { it.statnId }
    private val adj: Map<String, Set<String>>

    init {
        require(ordered.isNotEmpty()) { "역 목록이 비어 있습니다." }
        require(byId.size == ordered.size) { "중복된 statnId 가 있습니다." }
        require(ordered.map { it.order }.toSet().size == ordered.size) { "중복된 orderInLine 이 있습니다." }
        adj = if (explicitEdges != null) {
            buildMap<String, MutableSet<String>> {
                for (s in ordered) put(s.statnId, mutableSetOf())
                for ((a, b) in explicitEdges) {
                    require(byId.containsKey(a) && byId.containsKey(b)) {
                        "엣지에 없는 역: $a - $b"
                    }
                    getValue(a).add(b)
                    getValue(b).add(a)
                }
            }
        } else {
            // LINEAR: order 정렬 후 인접 연결
            buildMap<String, MutableSet<String>> {
                for (s in ordered) put(s.statnId, mutableSetOf())
                for (i in 0 until ordered.lastIndex) {
                    val a = ordered[i].statnId
                    val b = ordered[i + 1].statnId
                    getValue(a).add(b)
                    getValue(b).add(a)
                }
            }
        }
    }

    val size: Int get() = ordered.size
    val stations: List<Station> get() = ordered

    fun contains(statnId: String): Boolean = byId.containsKey(statnId)

    fun station(statnId: String): Station? = byId[statnId]

    /** 그래프상 인접 역(분기 포함). */
    fun adjacent(statnId: String): List<Station> =
        adj[statnId].orEmpty().mapNotNull { byId[it] }

    private fun require(statnId: String): Station =
        byId[statnId] ?: throw IllegalArgumentException("그래프에 없는 역: $statnId")

    /** origin → dest 최단 경로(양끝 포함). 없으면 null. */
    fun path(fromId: String, toId: String): List<Station>? {
        if (fromId == toId) return listOf(require(fromId))
        if (!contains(fromId) || !contains(toId)) return null
        val prev = mutableMapOf<String, String?>()
        val q = ArrayDeque<String>()
        q.add(fromId)
        prev[fromId] = null
        while (q.isNotEmpty()) {
            val cur = q.removeFirst()
            if (cur == toId) break
            for (nb in adj[cur].orEmpty()) {
                if (nb !in prev) {
                    prev[nb] = cur
                    q.add(nb)
                }
            }
        }
        if (toId !in prev) return null
        val ids = mutableListOf<String>()
        var c: String? = toId
        while (c != null) {
            ids.add(c)
            c = prev[c]
        }
        ids.reverse()
        return ids.map { byId.getValue(it) }
    }

    /**
     * 출발→목적 경로의 첫 Hop 기준 방향.
     * order 가 증가하는 쪽을 ASCENDING 으로 두어 updnLine 극성과 맞춘다(1호선 분기 포함).
     */
    fun directionBetween(originId: String, destId: String): Direction {
        val p = path(originId, destId)
            ?: throw IllegalArgumentException("경로 없음: $originId → $destId")
        require(p.size >= 2) { "출발역과 목적역이 동일합니다: $originId" }
        return if (p[1].order >= p[0].order) Direction.ASCENDING else Direction.DESCENDING
    }

    /**
     * [destId] 쪽으로 한 칸 진행. 경로가 없거나 종착이면 null.
     * direction 인자는 하위 호환용 — 경로가 있으면 dest 방향이 우선.
     */
    fun nextStation(currentId: String, direction: Direction, destId: String? = null): Station? {
        if (destId != null) {
            val p = path(currentId, destId) ?: return null
            return p.getOrNull(1)
        }
        // dest 없으면 order±1 (LINEAR 호환) 또는 인접 중 order 방향
        val c = require(currentId)
        val candidates = adj[currentId].orEmpty().mapNotNull { byId[it] }
        return when (direction) {
            Direction.ASCENDING -> candidates.filter { it.order >= c.order }.minByOrNull { it.order }
            Direction.DESCENDING -> candidates.filter { it.order <= c.order }.maxByOrNull { it.order }
        }
    }

    fun previousStation(currentId: String, direction: Direction, originId: String? = null): Station? {
        if (originId != null) {
            // origin→current 경로에서 한 칸 뒤
            val p = path(originId, currentId) ?: return null
            return if (p.size >= 2) p[p.lastIndex - 1] else null
        }
        return nextStation(currentId, direction.opposite, destId = null)
    }

    /**
     * 목적지 직전역(leadStations개 전). origin→dest 경로 기준.
     */
    fun stationBeforeArrival(
        destId: String,
        direction: Direction,
        leadStations: Int = 1,
        originId: String? = null,
    ): Station? {
        require(leadStations >= 1) { "leadStations 는 1 이상이어야 합니다: $leadStations" }
        if (originId != null) {
            val p = path(originId, destId) ?: return null
            val idx = p.lastIndex - leadStations
            return p.getOrNull(idx)
        }
        // LINEAR 폴백: direction 으로 역행
        var current = require(destId)
        repeat(leadStations) {
            current = previousStation(current.statnId, direction) ?: return null
        }
        return current
    }

    fun remainingStops(currentId: String, destId: String): Int {
        val p = path(currentId, destId) ?: return kotlin.math.abs(require(destId).order - require(currentId).order)
        return (p.size - 1).coerceAtLeast(0)
    }

    fun isValidTrip(originId: String, destId: String): Boolean {
        if (!contains(originId) || !contains(destId) || originId == destId) return false
        return path(originId, destId) != null
    }

    fun stationsBetween(originId: String, destId: String): List<Station> {
        if (!isValidTrip(originId, destId)) return emptyList()
        return path(originId, destId).orEmpty()
    }
}
