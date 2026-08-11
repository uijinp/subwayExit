package com.jin.subwayalarm.domain.model

/** 즐겨찾기한 경로 (출발역명 → 목적지역명). 실제 구간은 불러올 때 다시 계산한다. */
data class FavoriteRoute(
    val originName: String,
    val destinationName: String,
)
