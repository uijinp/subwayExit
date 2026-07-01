package com.jin.subwayalarm.data.repository

import com.jin.subwayalarm.BuildConfig
import com.jin.subwayalarm.data.remote.SubwayApiService
import com.jin.subwayalarm.data.remote.toDomainOrNull
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrainPositionRepositoryImpl @Inject constructor(
    private val api: SubwayApiService,
) : TrainPositionRepository {

    override suspend fun fetchRealtimePosition(lineName: String): PositionFetchResult {
        if (BuildConfig.SEOUL_API_KEY.isBlank()) {
            return PositionFetchResult.Error("SEOUL_API_KEY 가 비어 있습니다(local.properties 확인).")
        }
        return try {
            val response = api.realtimePosition(BuildConfig.SEOUL_API_KEY, START_INDEX, END_INDEX, lineName)
            val err = response.errorMessage
            when {
                err != null && (err.status == HTTP_TOO_MANY_REQUESTS || err.code == RATE_LIMIT_CODE) ->
                    PositionFetchResult.RateLimited
                err != null && err.status != null && err.status != HTTP_OK ->
                    PositionFetchResult.Error(err.message ?: "API 오류(${err.status})")
                else ->
                    PositionFetchResult.Success(response.realtimePositionList.orEmpty().mapNotNull { it.toDomainOrNull() })
            }
        } catch (e: HttpException) {
            if (e.code() == HTTP_TOO_MANY_REQUESTS) {
                PositionFetchResult.RateLimited
            } else {
                PositionFetchResult.Error("HTTP ${e.code()}")
            }
        } catch (e: IOException) {
            PositionFetchResult.Error(e.message ?: "네트워크 오류")
        }
    }

    private companion object {
        const val START_INDEX = 1
        const val END_INDEX = 100
        const val HTTP_OK = 200
        const val HTTP_TOO_MANY_REQUESTS = 429
        // ⚠️ 실제 일일한도초과 오류 코드는 미검증(작업3에서 확정). 우선 흔한 관례값을 추정치로 둠.
        const val RATE_LIMIT_CODE = "INFO-200"
    }
}
