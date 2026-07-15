package com.jin.subwayalarm.data.remote

import com.jin.subwayalarm.data.remote.dto.TrainPositionDto
import com.jin.subwayalarm.domain.model.TrainPosition
import com.jin.subwayalarm.domain.model.TrainStatus

/**
 * DTO → 도메인 매핑.
 *
 * 실시간 API 필드 안정성이 작업순서 3(실측 검증 스파이크)으로 아직 미검증이므로(R-2),
 * 필수 필드 누락이나 trainSttus 코드 불명 시 크래시 대신 null 로 스킵한다(방어적 경계).
 */
fun TrainPositionDto.toDomainOrNull(): TrainPosition? {
    val lineIdValue = subwayId ?: return null
    val trainNoValue = trainNo ?: return null
    val statnIdValue = statnId ?: return null
    val statnNameValue = statnNm ?: return null
    val updnLineValue = updnLine ?: return null
    val recptnDtValue = recptnDt ?: return null
    val statusCode = trainSttus?.trim()?.toIntOrNull() ?: return null
    val status = TrainStatus.fromCode(statusCode) ?: return null
    val recptnDtMillis = runCatching { RecptnDtParser.parseToEpochMillis(recptnDtValue) }
        .getOrNull() ?: return null

    return TrainPosition(
        lineId = lineIdValue,
        trainNo = trainNoValue,
        statnId = statnIdValue,
        statnName = statnNameValue,
        updnLine = updnLineValue,
        status = status,
        isExpress = directAt == "1",
        isLastTrain = lstcarAt == "1",
        recptnDtEpochMillis = recptnDtMillis,
        terminusName = statnTnm?.trim()?.takeIf { it.isNotEmpty() },
    )
}
