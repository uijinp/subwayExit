package com.jin.subwayalarm.data.repository

import com.jin.subwayalarm.domain.model.TripSession
import kotlinx.coroutines.flow.Flow

interface TripSessionRepository {
    /** 영속화된 진행 중 여정. 없으면 null(사망 복구 시 이 값으로 판단). */
    suspend fun current(): TripSession?

    /** UI(TrackingScreen/AlarmScreen)가 실시간 반영을 위해 관찰. */
    fun observeCurrent(): Flow<TripSession?>

    suspend fun save(session: TripSession)

    suspend fun clear()
}
