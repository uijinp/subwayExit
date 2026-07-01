package com.jin.subwayalarm.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * 서울 열린데이터광장 realtimePosition 응답 DTO.
 *
 * 작업순서 3(2026-07-01) 실측으로 아래 필드는 실응답과 일치 확인됨(7호선 44건 샘플 +
 * 75초 간격 3회 시계열). 응답에는 이 외에도 subwayNm(노선명), statnTid/statnTnm
 * (해당 열차의 종착역 ID/명 — updnLine 극성 교차검증에 사용, SubwaySeed 참조),
 * lastRecptnDt(날짜만), 페이징 메타(rowNum 등) 필드가 더 있으나 Moshi 는 미선언 필드를
 * 안전하게 무시하므로 별도 매핑하지 않았다.
 */
@JsonClass(generateAdapter = true)
data class RealtimePositionResponseDto(
    @Json(name = "errorMessage") val errorMessage: ApiErrorMessageDto? = null,
    @Json(name = "realtimePositionList") val realtimePositionList: List<TrainPositionDto>? = null,
)

@JsonClass(generateAdapter = true)
data class ApiErrorMessageDto(
    @Json(name = "status") val status: Int? = null,
    @Json(name = "code") val code: String? = null,
    @Json(name = "message") val message: String? = null,
)

@JsonClass(generateAdapter = true)
data class TrainPositionDto(
    @Json(name = "subwayId") val subwayId: String? = null,
    @Json(name = "trainNo") val trainNo: String? = null,
    @Json(name = "statnNm") val statnNm: String? = null,
    @Json(name = "statnId") val statnId: String? = null,
    @Json(name = "updnLine") val updnLine: String? = null,
    @Json(name = "trainSttus") val trainSttus: String? = null,
    @Json(name = "directAt") val directAt: String? = null,
    @Json(name = "lstcarAt") val lstcarAt: String? = null,
    @Json(name = "recptnDt") val recptnDt: String? = null,
)
