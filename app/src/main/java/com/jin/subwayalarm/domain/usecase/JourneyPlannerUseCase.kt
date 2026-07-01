package com.jin.subwayalarm.domain.usecase

import com.jin.subwayalarm.domain.model.Leg
import com.jin.subwayalarm.domain.model.SubwayNetwork
import java.util.PriorityQueue
import javax.inject.Inject

/**
 * 출발역→목적역 최단 경로를 계산해 구간(Leg) 목록으로 반환 (환승 지원, 순수 로직).
 *
 * 상태를 (역명, 현재 타고 있는 노선ID) 로 둬서, 같은 역이라도 "어느 노선으로 들어왔는지"를
 * 구분한다 — 그래야 "노선을 바꾸는 것"에만 환승 비용을 매길 수 있다(그냥 역명만 그래프
 * 노드로 두면 환승 여부를 구분 못 함). 같은 노선 한 정거장 이동 비용=1, 환승 비용=
 * [TRANSFER_PENALTY] — 웬만한 우회보다 환승이 손해라 불필요한 환승을 피하되, 정말
 * 필요한 환승(직행이 없는 경우)은 선택하게 한다.
 */
class JourneyPlannerUseCase @Inject constructor() {

    operator fun invoke(network: SubwayNetwork, originName: String, destName: String): List<Leg>? {
        if (originName == destName) return null
        val originLines = network.linesServing(originName)
        val destLines = network.linesServing(destName)
        if (originLines.isEmpty() || destLines.isEmpty()) return null

        data class State(val stationName: String, val lineId: String)

        val dist = mutableMapOf<State, Int>()
        val prev = mutableMapOf<State, State>()
        val queue = PriorityQueue<Pair<State, Int>>(compareBy { it.second })

        for (lineId in originLines) {
            val start = State(originName, lineId)
            dist[start] = 0
            queue.add(start to 0)
        }

        var goal: State? = null
        while (queue.isNotEmpty()) {
            val (state, cost) = queue.poll()!!
            if (cost > (dist[state] ?: Int.MAX_VALUE)) continue
            if (state.stationName == destName) {
                goal = state
                break
            }
            for (edge in network.neighbors(state.stationName, state.lineId)) {
                val next = State(edge.stationName, edge.lineId)
                val edgeCost = if (edge.isTransfer) TRANSFER_PENALTY else 1
                val newCost = cost + edgeCost
                if (newCost < (dist[next] ?: Int.MAX_VALUE)) {
                    dist[next] = newCost
                    prev[next] = state
                    queue.add(next to newCost)
                }
            }
        }

        val goalState = goal ?: return null

        // 경로 복원(역순) 후 정순으로.
        val path = mutableListOf(goalState)
        var cur = goalState
        while (true) {
            val p = prev[cur] ?: break
            path.add(p)
            cur = p
        }
        path.reverse()

        // 연속된 같은-lineId 구간을 하나의 Leg 로 묶는다.
        val legs = mutableListOf<Leg>()
        var legStart = path.first()
        for (i in 1 until path.size) {
            val prevState = path[i - 1]
            val state = path[i]
            if (state.lineId != prevState.lineId) {
                legs.add(
                    Leg(
                        lineId = legStart.lineId,
                        lineName = network.lineName(legStart.lineId),
                        boardStationName = legStart.stationName,
                        alightStationName = prevState.stationName,
                    ),
                )
                legStart = state
            }
        }
        legs.add(
            Leg(
                lineId = legStart.lineId,
                lineName = network.lineName(legStart.lineId),
                boardStationName = legStart.stationName,
                alightStationName = path.last().stationName,
            ),
        )
        return legs
    }

    private companion object {
        const val TRANSFER_PENALTY = 6
    }
}
