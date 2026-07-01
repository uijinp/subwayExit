package com.jin.subwayalarm.data.repository

import com.jin.subwayalarm.domain.model.TripSession

interface TripSessionRepository {
    /** 영속화된 진행 중 여정. 없으면 null(사망 복구 시 이 값으로 판단). */
    suspend fun current(): TripSession?

    suspend fun save(session: TripSession)

    suspend fun clear()
}
