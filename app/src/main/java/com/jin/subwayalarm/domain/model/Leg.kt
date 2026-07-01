package com.jin.subwayalarm.domain.model

import com.squareup.moshi.JsonClass

/** 여정의 한 구간(같은 노선으로 이동). 환승 없는 여정도 1개짜리 Leg 리스트로 표현. */
@JsonClass(generateAdapter = true)
data class Leg(
    val lineId: String,
    val lineName: String,
    val boardStationName: String,
    val alightStationName: String,
)
