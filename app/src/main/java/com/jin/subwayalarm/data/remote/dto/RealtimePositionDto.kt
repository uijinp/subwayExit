package com.jin.subwayalarm.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * 서울 열린데이터광장 realtimePosition 응답 DTO.
 * ⚠️ 필드명·구조는 설계 기준값 — 작업순서 3(실측 검증 스파이크)에서
 * 최신 Swagger/공공데이터포털 명세로 재검증할 것 (2-2 참조).
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
