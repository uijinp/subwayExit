package com.jin.subwayalarm.domain.model

/** (역명, 그 역을 지나는 노선ID) 로 이동 가능한 이웃 하나. isTransfer=true 면 같은 역에서 다른 노선으로 갈아타는 것. */
data class NetworkEdge(val stationName: String, val lineId: String, val isTransfer: Boolean)

/**
 * 여러 노선을 역명 기준으로 연결한 네트워크 그래프 (순수 로직, 환승 경로 탐색의 토대).
 *
 * 노선별 statnId 는 서로 다른 네임스페이스라 물리적으로 같은 역을 잇는 기본 키는 역명이다.
 * 표기가 다른 동일 환승역(예: 이수 ↔ 총신대입구(이수))은 [WalkTransfers] 별칭으로 연결한다.
 */
class SubwayNetwork(
    perLineStations: Map<String, List<Station>>,
    lineMetas: List<LineMeta>,
    walkTransfers: Map<String, Set<String>> = WalkTransfers.aliasesOf,
) {

    private val lineNameById: Map<String, String> = lineMetas.associate { it.lineId to it.lineName }
    private val displayNameById: Map<String, String> =
        lineMetas.associate { it.lineId to (it.displayName.ifBlank { it.lineName }) }
    private val graphs: Map<String, StationGraph> = perLineStations
        .filterValues { it.isNotEmpty() }
        .mapValues { (_, stations) -> StationGraph(stations) }

    /** 역명 → 그 역을 지나는 노선ID 집합. */
    private val linesByName: Map<String, Set<String>> = buildMap<String, MutableSet<String>> {
        for ((lineId, stations) in perLineStations) {
            for (station in stations) {
                getOrPut(station.name) { mutableSetOf() }.add(lineId)
            }
        }
    }

    private val aliases: Map<String, Set<String>> = walkTransfers

    fun linesServing(name: String): Set<String> = linesByName[name].orEmpty()

    fun lineName(lineId: String): String = lineNameById[lineId].orEmpty()

    /** UI용 표시명 (1호선 경인 구간 고지 등). */
    fun displayName(lineId: String): String = displayNameById[lineId] ?: lineName(lineId)

    fun stationOn(lineId: String, name: String): Station? =
        graphs[lineId]?.stations?.firstOrNull { it.name == name }

    /**
     * (stationName, lineId) 의 이웃: 같은 노선 인접역(양방향) + 같은 역명 환승 + 별칭 보행 환승.
     */
    fun neighbors(stationName: String, lineId: String): List<NetworkEdge> {
        val graph = graphs[lineId] ?: return emptyList()
        val station = stationOn(lineId, stationName) ?: return emptyList()

        val sameLine = listOfNotNull(
            graph.nextStation(station.statnId, Direction.ASCENDING),
            graph.nextStation(station.statnId, Direction.DESCENDING),
        ).map { NetworkEdge(it.name, lineId, isTransfer = false) }

        val sameNameTransfers = linesServing(stationName)
            .filter { it != lineId }
            .map { NetworkEdge(stationName, it, isTransfer = true) }

        // 별칭 환승: 다른 표기의 역명으로 그 역을 지나는 노선에 연결 (예: 이수 → 총신대입구(이수)·4호선)
        val aliasTransfers = aliases[stationName].orEmpty().flatMap { aliasName ->
            linesServing(aliasName)
                .filter { it != lineId }
                .map { NetworkEdge(aliasName, it, isTransfer = true) }
        }

        return sameLine + sameNameTransfers + aliasTransfers
    }
}
