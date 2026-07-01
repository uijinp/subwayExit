package com.jin.subwayalarm.domain.model

/** (역명, 그 역을 지나는 노선ID) 로 이동 가능한 이웃 하나. isTransfer=true 면 같은 역에서 다른 노선으로 갈아타는 것. */
data class NetworkEdge(val stationName: String, val lineId: String, val isTransfer: Boolean)

/**
 * 여러 노선을 역명 기준으로 연결한 네트워크 그래프 (순수 로직, 환승 경로 탐색의 토대).
 *
 * 노선별 statnId 는 서로 다른 네임스페이스라 물리적으로 같은 역을 잇는 유일한 키는 역명이다
 * (설계 1-6 연장: 그래프 확장 시에도 알고리즘을 갈아엎지 않도록 순수 함수로 유지).
 */
class SubwayNetwork(perLineStations: Map<String, List<Station>>, lineMetas: List<LineMeta>) {

    private val lineNameById: Map<String, String> = lineMetas.associate { it.lineId to it.lineName }
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

    fun linesServing(name: String): Set<String> = linesByName[name].orEmpty()

    fun lineName(lineId: String): String = lineNameById[lineId].orEmpty()

    fun stationOn(lineId: String, name: String): Station? =
        graphs[lineId]?.stations?.firstOrNull { it.name == name }

    /**
     * (stationName, lineId) 의 이웃: 같은 노선 인접역(양방향) + 같은 역명의 다른 노선(환승, 비용은
     * 호출측/탐색 알고리즘이 매김).
     */
    fun neighbors(stationName: String, lineId: String): List<NetworkEdge> {
        val graph = graphs[lineId] ?: return emptyList()
        val station = stationOn(lineId, stationName) ?: return emptyList()

        val sameLine = listOfNotNull(
            graph.nextStation(station.statnId, Direction.ASCENDING),
            graph.nextStation(station.statnId, Direction.DESCENDING),
        ).map { NetworkEdge(it.name, lineId, isTransfer = false) }

        val transfers = linesServing(stationName)
            .filter { it != lineId }
            .map { NetworkEdge(stationName, it, isTransfer = true) }

        return sameLine + transfers
    }
}
