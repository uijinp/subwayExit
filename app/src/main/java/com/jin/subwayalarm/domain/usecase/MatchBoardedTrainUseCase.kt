package com.jin.subwayalarm.domain.usecase

import com.jin.subwayalarm.domain.model.BoardingCandidate
import com.jin.subwayalarm.domain.model.Direction
import com.jin.subwayalarm.domain.model.DirectionPolarity
import com.jin.subwayalarm.domain.model.StationGraph
import com.jin.subwayalarm.domain.model.TrainPosition
import com.jin.subwayalarm.domain.model.TrainStatus
import javax.inject.Inject

sealed class BoardingMatch {
    data class Matched(val trainNo: String) : BoardingMatch()
    data object NoCandidate : BoardingMatch()
    data class Ambiguous(val candidates: List<BoardingCandidate>) : BoardingMatch()
}

/**
 * 탑승 열차 특정 (3-3):
 * 출발역 + 방향 일치 + (가능하면) 종착이 목적지를 지나가는 열차.
 *
 * 1호선 예: 동두천→천안 이면 인천행을 걸러 천안·신창행을 고른다.
 * 종착 정보를 모르면(null) 후보에 남긴다.
 */
class MatchBoardedTrainUseCase @Inject constructor() {

    fun candidates(
        positions: List<TrainPosition>,
        lineId: String,
        originStatnId: String,
        direction: Direction,
        polarity: DirectionPolarity,
        excludedTerminusNames: Set<String> = emptySet(),
        graph: StationGraph? = null,
        destStatnId: String? = null,
    ): List<BoardingCandidate> {
        val base = positions.asSequence()
            .filter { pos ->
                pos.lineId == lineId &&
                    pos.statnId == originStatnId &&
                    polarity.directionOf(pos.updnLine) == direction &&
                    pos.status in BOARDING_CANDIDATE_STATUSES &&
                    !isExcludedTerminus(pos.terminusName, excludedTerminusNames)
            }
            .map { pos ->
                BoardingCandidate(
                    trainNo = pos.trainNo,
                    statnId = pos.statnId,
                    statnName = pos.statnName,
                    status = pos.status,
                    updnLine = pos.updnLine,
                    direction = polarity.directionOf(pos.updnLine),
                    terminusName = pos.terminusName,
                    isExpress = pos.isExpress,
                    recptnDtEpochMillis = pos.recptnDtEpochMillis,
                )
            }
            .groupBy { it.trainNo }
            .map { (_, rows) -> rows.maxBy { it.recptnDtEpochMillis } }
            .toList()

        if (graph == null || destStatnId == null) {
            return base.sortedBy { it.trainNo }
        }

        // 목적지가 종착 경로 위에 있는 후보 우선. 그런 후보가 1개 이상이면 그것만 사용.
        val compatible = base.filter { c ->
            terminusServesDestination(graph, originStatnId, destStatnId, c.terminusName)
        }
        return (if (compatible.isNotEmpty()) compatible else base).sortedBy { it.trainNo }
    }

    operator fun invoke(
        positions: List<TrainPosition>,
        lineId: String,
        originStatnId: String,
        direction: Direction,
        polarity: DirectionPolarity,
        excludedTerminusNames: Set<String> = emptySet(),
        graph: StationGraph? = null,
        destStatnId: String? = null,
    ): BoardingMatch {
        val list = candidates(
            positions, lineId, originStatnId, direction, polarity,
            excludedTerminusNames, graph, destStatnId,
        )
        val only = list.singleOrNull()
        return when {
            list.isEmpty() -> BoardingMatch.NoCandidate
            only != null && isAutoConfirmable(only, graph, originStatnId, destStatnId) -> BoardingMatch.Matched(only.trainNo)
            else -> BoardingMatch.Ambiguous(list)
        }
    }

    /**
     * 후보가 하나뿐이어도 목적지에 가지 않는 게 확실한 열차는 자동 확정하지 않는다.
     * [candidates] 는 적합 후보가 없으면 수동 선택용으로 부적합 후보도 돌려주는데, 예전엔 그
     * 하나를 그대로 확정해 — 서울역→부평에서 천안행만 있을 때 — 구로에서 갈라져 알람이 영영
     * 안 울렸다. 적합한 다음 열차가 오면 그때 자동 확정된다.
     */
    fun isAutoConfirmable(
        candidate: BoardingCandidate,
        graph: StationGraph?,
        originStatnId: String,
        destStatnId: String?,
    ): Boolean {
        if (graph == null || destStatnId == null) return true
        return terminusServesDestination(graph, originStatnId, destStatnId, candidate.terminusName)
    }

    /**
     * 열차가 origin 에서 terminus 로 갈 때 dest 를 지나는지.
     * 종착을 그래프에서 못 찾으면 보수적으로 true(필터 안 함).
     *
     * 열차 경로는 "목적지 쪽 다음 역"에서 출발역을 거치지 않고 종착까지 가는 길로 본다.
     * 최단 경로만 보면 순환선(2호선)에서 반대로 도는 쪽이 더 짧은 종착(강남→사당 방향의 성수행)을
     * 목적지에 안 간다고 오판한다. 분기·직선 노선에서는 최단 경로와 같은 결과다.
     */
    fun terminusServesDestination(
        graph: StationGraph,
        originStatnId: String,
        destStatnId: String,
        terminusName: String?,
    ): Boolean {
        if (terminusName.isNullOrBlank()) return true
        val term = findStationByTerminusName(graph, terminusName) ?: return true
        if (term.statnId == destStatnId) return true
        val next = graph.path(originStatnId, destStatnId)?.getOrNull(1) ?: return true
        if (next.statnId == term.statnId) return false
        val route = graph.path(next.statnId, term.statnId, avoid = originStatnId) ?: return false
        return route.any { it.statnId == destStatnId }
    }

    private fun findStationByTerminusName(graph: StationGraph, terminusName: String) =
        graph.stations.firstOrNull { st ->
            val n = st.name
            n == terminusName ||
                n.startsWith(terminusName) ||
                terminusName.startsWith(n.substringBefore('(')) ||
                // API 는 "서울", 시드는 "서울역"
                n.removeSuffix("역") == terminusName.removeSuffix("역")
        }

    private fun isExcludedTerminus(terminusName: String?, excluded: Set<String>): Boolean {
        if (terminusName.isNullOrBlank() || excluded.isEmpty()) return false
        val t = terminusName.trim()
        return excluded.any { ex -> t == ex || t.startsWith(ex) || t.contains(ex) }
    }

    private companion object {
        val BOARDING_CANDIDATE_STATUSES = setOf(
            TrainStatus.APPROACHING,
            TrainStatus.ARRIVED,
            TrainStatus.DEPARTED,
        )
    }
}
