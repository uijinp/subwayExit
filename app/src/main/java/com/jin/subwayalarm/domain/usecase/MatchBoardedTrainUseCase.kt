package com.jin.subwayalarm.domain.usecase

import com.jin.subwayalarm.domain.model.BoardingCandidate
import com.jin.subwayalarm.domain.model.Direction
import com.jin.subwayalarm.domain.model.DirectionPolarity
import com.jin.subwayalarm.domain.model.TrainPosition
import com.jin.subwayalarm.domain.model.TrainStatus
import javax.inject.Inject

sealed class BoardingMatch {
    data class Matched(val trainNo: String) : BoardingMatch()
    data object NoCandidate : BoardingMatch()
    data class Ambiguous(val candidates: List<BoardingCandidate>) : BoardingMatch()
}

/**
 * 탑승 열차 특정 (3-3, MVP 단순 가정):
 * "출발역에서 방향 일치 + 도착/진입/출발 상태인 열차".
 *
 * 후보 0 → NoCandidate, 1 → Matched, 2+ → Ambiguous(수동 선택).
 * [excludedTerminusNames] 로 1호선 경부 종착 등 잘못 분기 후보를 걸러낸다 (D-4).
 */
class MatchBoardedTrainUseCase @Inject constructor() {

    fun candidates(
        positions: List<TrainPosition>,
        lineId: String,
        originStatnId: String,
        direction: Direction,
        polarity: DirectionPolarity,
        excludedTerminusNames: Set<String> = emptySet(),
    ): List<BoardingCandidate> {
        return positions.asSequence()
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
            // trainNo 중복(동일 열차 중복 행) 제거 — 최신 recptnDt 우선
            .groupBy { it.trainNo }
            .map { (_, rows) -> rows.maxBy { it.recptnDtEpochMillis } }
            .sortedBy { it.trainNo }
            .toList()
    }

    operator fun invoke(
        positions: List<TrainPosition>,
        lineId: String,
        originStatnId: String,
        direction: Direction,
        polarity: DirectionPolarity,
        excludedTerminusNames: Set<String> = emptySet(),
    ): BoardingMatch {
        val list = candidates(
            positions, lineId, originStatnId, direction, polarity, excludedTerminusNames,
        )
        return when (list.size) {
            0 -> BoardingMatch.NoCandidate
            1 -> BoardingMatch.Matched(list.first().trainNo)
            else -> BoardingMatch.Ambiguous(list)
        }
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
