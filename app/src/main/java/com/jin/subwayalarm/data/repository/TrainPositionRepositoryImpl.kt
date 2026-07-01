package com.jin.subwayalarm.data.repository

import com.jin.subwayalarm.BuildConfig
import com.jin.subwayalarm.data.remote.SubwayApiService
import com.jin.subwayalarm.data.remote.toDomainOrNull
import com.squareup.moshi.JsonDataException
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
                err != null && err.status == HTTP_TOO_MANY_REQUESTS -> PositionFetchResult.RateLimited
                // INFO-200 은 "해당하는 데이터 없음"(운행 중인 열차 없음)일 뿐 한도초과가 아니다(작업3 실측 확인).
                // 서울 열린데이터광장은 일일한도초과에 대한 별도 비즈니스 코드를 문서화하지 않았고
                // HTTP 레벨(429)로만 신호를 준다 — 그 외 비-200 코드만 일반 오류로 처리한다.
                err != null && err.status != null && err.status != HTTP_OK && err.code != NO_DATA_CODE ->
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
        } catch (e: JsonDataException) {
            // 예상 밖 응답(HTML 오류 페이지 등)으로 파싱 실패 — 추적 루프를 죽이지 않고 오류로 흡수.
            PositionFetchResult.Error("응답 파싱 실패: ${e.message}")
        }
    }

    private companion object {
        const val START_INDEX = 1
        const val END_INDEX = 100
        const val HTTP_OK = 200
        const val HTTP_TOO_MANY_REQUESTS = 429
        // 실측 확인(작업3): "해당하는 데이터가 없습니다" — 열차 없음(정상 상황), 한도초과 아님.
        const val NO_DATA_CODE = "INFO-200"
    }
}
