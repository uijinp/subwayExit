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
        return when (list.size) {
            0 -> BoardingMatch.NoCandidate
            1 -> BoardingMatch.Matched(list.first().trainNo)
            else -> BoardingMatch.Ambiguous(list)
        }
    }

    /**
     * 열차가 origin 에서 terminus 로 갈 때 dest 를 지나는지.
     * 종착을 그래프에서 못 찾으면 보수적으로 true(필터 안 함).
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
        val pathToTerm = graph.path(originStatnId, term.statnId) ?: return true
        return pathToTerm.any { it.statnId == destStatnId }
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
