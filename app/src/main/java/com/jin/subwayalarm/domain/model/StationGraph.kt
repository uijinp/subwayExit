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

    /**
     * origin → dest 최단 경로(양끝 포함). 없으면 null.
     * [avoid] 역은 지나지 않는다 — 순환선에서 "이 방향으로 돌아가는" 경로를 구할 때 쓴다.
     */
    fun path(fromId: String, toId: String, avoid: String? = null): List<Station>? {
        if (fromId == toId) return listOf(require(fromId))
        if (!contains(fromId) || !contains(toId)) return null
        val prev = mutableMapOf<String, String?>()
        val q = ArrayDeque<String>()
        q.add(fromId)
        prev[fromId] = null
        if (avoid != null && avoid != fromId && avoid != toId) prev[avoid] = null
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
     * 출발→목적 경로의 진행 방향. order 가 증가하는 쪽이 ASCENDING(updnLine 극성과 짝).
     *
     * 순번이 정확히 한 칸 차이인 첫 hop 으로 정한다. 분기 접속(2호선 성수 10 → 용답 43)이나
     * 순환을 닫는 간선(2호선 충정로 42 ↔ 시청 0)은 순번이 크게 뛰어, 첫 hop 만 보면
     * 시청에서 출발할 때 양방향이 모두 ASCENDING 으로 나오는 사고가 있었다.
     * 한 칸 hop 이 하나도 없으면(시청↔충정로 1정거장 여정 등) 그 간선이 순환을 닫는
     * 간선인지 보고 부호를 뒤집는다.
     */
    fun directionBetween(originId: String, destId: String): Direction {
        val p = path(originId, destId)
            ?: throw IllegalArgumentException("경로 없음: $originId → $destId")
        require(p.size >= 2) { "출발역과 목적역이 동일합니다: $originId" }
        for (i in 0 until p.lastIndex) {
            when (p[i + 1].order - p[i].order) {
                1 -> return Direction.ASCENDING
                -1 -> return Direction.DESCENDING
            }
        }
        val a = p[0]
        val b = p[1]
        val ascending = (b.order > a.order) != isLoopClosingEdge(a, b)
        return if (ascending) Direction.ASCENDING else Direction.DESCENDING
    }

    /** 최소 순번 역이 양쪽으로 이어진 순환선에서, 그 역에 붙은 순번 비인접 간선. */
    private fun isLoopClosingEdge(a: Station, b: Station): Boolean {
        val first = ordered.first()
        if (a.statnId != first.statnId && b.statnId != first.statnId) return false
        return adj[first.statnId].orEmpty().size >= 2 && kotlin.math.abs(a.order - b.order) > 1
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

    /**
     * 남은 정거장 수. 그래프에 없는 역이 들어오면 예외 대신 0 — 추적 루프·화면이 이 값을
     * 표시·폴링 주기에 쓰는데, 예외가 나면 서비스가 죽어 재시작마다 다시 죽었다.
     */
    fun remainingStops(currentId: String, destId: String): Int {
        if (!contains(currentId) || !contains(destId)) return 0
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
