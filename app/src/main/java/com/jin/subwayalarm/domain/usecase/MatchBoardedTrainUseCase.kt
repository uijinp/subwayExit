package com.jin.subwayalarm.domain.usecase

import com.jin.subwayalarm.domain.model.Direction
import com.jin.subwayalarm.domain.model.DirectionPolarity
import com.jin.subwayalarm.domain.model.TrainPosition
import com.jin.subwayalarm.domain.model.TrainStatus
import javax.inject.Inject

sealed class BoardingMatch {
    data class Matched(val trainNo: String) : BoardingMatch()
    data object NoCandidate : BoardingMatch()
    data class Ambiguous(val candidateTrainNos: List<String>) : BoardingMatch()
}

/**
 * 탑승 열차 특정 (3-3, MVP 단순 가정):
 * "출발역에서 방향 일치 + 도착/진입/출발 상태인 열차 = 직후 출발 열차".
 *
 * 다중 후보 수렴(가속도계 정차 패턴 대조)은 2차(작업순서 10)에서 강화한다.
 * 지금은 후보가 정확히 1개일 때만 확정하고, 0개·다수면 상위 계층(추적 루프)이
 * MATCHING 상태를 유지하며 다음 폴링에서 재시도한다.
 */
class MatchBoardedTrainUseCase @Inject constructor() {
    operator fun invoke(
        positions: List<TrainPosition>,
        lineId: String,
        originStatnId: String,
        direction: Direction,
        polarity: DirectionPolarity,
    ): BoardingMatch {
        val candidates = positions.filter { pos ->
            pos.lineId == lineId &&
                pos.statnId == originStatnId &&
                polarity.directionOf(pos.updnLine) == direction &&
                pos.status in BOARDING_CANDIDATE_STATUSES
        }
        return when (candidates.size) {
            0 -> BoardingMatch.NoCandidate
            1 -> BoardingMatch.Matched(candidates.first().trainNo)
            else -> BoardingMatch.Ambiguous(candidates.map { it.trainNo })
        }
    }

    private companion object {
        val BOARDING_CANDIDATE_STATUSES = setOf(
            TrainStatus.APPROACHING,
            TrainStatus.ARRIVED,
            TrainStatus.DEPARTED,
        )
    }
}
