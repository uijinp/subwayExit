package com.jin.subwayalarm.data.repository

import com.jin.subwayalarm.domain.model.TrainPosition

sealed class PositionFetchResult {
    data class Success(val positions: List<TrainPosition>) : PositionFetchResult()

    /** 일일 호출 한도 초과 등(2-7, R-1) — 지수 백오프 대상. */
    data object RateLimited : PositionFetchResult()

    data class Error(val message: String) : PositionFetchResult()
}

interface TrainPositionRepository {
    suspend fun fetchRealtimePosition(lineName: String): PositionFetchResult
}
