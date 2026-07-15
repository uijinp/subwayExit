package com.jin.subwayalarm.domain.usecase

import com.jin.subwayalarm.domain.model.Direction
import com.jin.subwayalarm.domain.model.DirectionPolarity
import com.jin.subwayalarm.domain.model.Station
import com.jin.subwayalarm.domain.model.TrainPosition
import com.jin.subwayalarm.domain.model.TrainStatus
import javax.inject.Inject

/** 노선 현황판에 표시할 열차 하나. */
data class LineTrainMarker(
    val trainNo: String,
    /** null = 노선 극성 미검증 또는 updnLine 코드 불명 — 방향 표시 생략. */
    val direction: Direction?,
    val status: TrainStatus,
    val isExpress: Boolean,
    val isLastTrain: Boolean,
)

/** 노선 현황판의 역 한 줄: 역 + 지금 그 역에 있는 열차들. */
data class LineBoardRow(
    val station: Station,
    val trains: List<LineTrainMarker>,
)

/**
 * 실시간 위치 응답을 노선 현황판(역 순서대로 나열 + 역별 열차 목록)으로 변환 (순수 로직).
 *
 * 시드 구간 밖의 역(예: 1호선 경인선 구간 밖)을 달리는 열차는 표시할 줄이 없어 제외되고,
 * 제외 수는 호출측이 [totalOnLine]과 표시 합계의 차로 계산한다.
 */
class BuildLineBoardUseCase @Inject constructor() {

    operator fun invoke(
        stations: List<Station>,
        polarity: DirectionPolarity?,
        positions: List<TrainPosition>,
        lineId: String,
    ): List<LineBoardRow> {
        // 실측(2026-07-15, 1호선): API 가 같은 trainNo 를 recptnDt 만 다른 중복 행으로 줄 수 있다
        // (79행 중 9대 중복 관측). 열차당 최신 수신 행만 남긴다.
        val byStatnId = positions.filter { it.lineId == lineId }
            .groupBy { it.trainNo }
            .map { (_, rows) -> rows.maxBy { it.recptnDtEpochMillis } }
            .groupBy { it.statnId }
        return stations.sortedBy { it.order }.map { station ->
            val markers = byStatnId[station.statnId].orEmpty().map { pos ->
                LineTrainMarker(
                    trainNo = pos.trainNo,
                    direction = polarity?.directionOf(pos.updnLine),
                    status = pos.status,
                    isExpress = pos.isExpress,
                    isLastTrain = pos.isLastTrain,
                )
            }
            LineBoardRow(station = station, trains = markers)
        }
    }
}
