package com.jin.subwayalarm.replay

import com.jin.subwayalarm.domain.model.TrainPosition
import com.jin.subwayalarm.domain.model.TrainStatus
import com.jin.subwayalarm.domain.usecase.AlertEvaluation

/**
 * Replay 픽스처 스키마 (순수 모델 — JSON 로더는 테스트/Moshi 쪽에서 매핑).
 */
data class ReplayFixture(
    val meta: ReplayMeta,
    val trip: ReplayTrip,
    val frames: List<ReplayFrame>,
    val expect: ReplayExpect? = null,
)

data class ReplayMeta(
    val lineName: String,
    val lineId: String,
    val description: String = "",
)

data class ReplayTrip(
    val originStatnId: String,
    val destStatnId: String,
    val leadStations: Int = 1,
)

data class ReplayFrame(
    val tOffsetSec: Long,
    val positions: List<ReplayPosition>,
)

data class ReplayPosition(
    val trainNo: String,
    val statnId: String,
    val statnNm: String,
    val updnLine: String,
    val trainSttus: Int,
    val recptnDtEpochMillis: Long,
    val terminusName: String? = null,
) {
    fun toDomain(lineId: String): TrainPosition? {
        val status = TrainStatus.fromCode(trainSttus) ?: return null
        return TrainPosition(
            lineId = lineId,
            trainNo = trainNo,
            statnId = statnId,
            statnName = statnNm,
            updnLine = updnLine,
            status = status,
            isExpress = false,
            isLastTrain = false,
            recptnDtEpochMillis = recptnDtEpochMillis,
            terminusName = terminusName,
        )
    }
}

data class ReplayExpect(
    val finalState: String,
    val boardedTrainNo: String? = null,
    val alertRule: String? = null,
)

/** 가상 시각 — 프레임 tOffset 기준. */
class ReplayClock(private val baseEpochMillis: Long = 1_700_000_000_000L) {
    fun nowAt(tOffsetSec: Long): Long = baseEpochMillis + tOffsetSec * 1000L
}
