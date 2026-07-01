package com.jin.subwayalarm.data.remote

import com.jin.subwayalarm.data.remote.dto.RealtimePositionResponseDto
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * 서울 열린데이터광장 실시간 API (2-2). ⚠️ 경로·파라미터는 설계 기준값 —
 * 작업순서 3(실측 검증 스파이크)에서 최신 명세로 재검증할 것.
 */
interface SubwayApiService {

    @GET("{apiKey}/json/realtimePosition/{startIndex}/{endIndex}/{lineName}")
    suspend fun realtimePosition(
        @Path("apiKey") apiKey: String,
        @Path("startIndex") startIndex: Int,
        @Path("endIndex") endIndex: Int,
        @Path("lineName") lineName: String,
    ): RealtimePositionResponseDto
}
