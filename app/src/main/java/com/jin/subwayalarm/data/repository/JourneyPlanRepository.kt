package com.jin.subwayalarm.data.repository

import com.jin.subwayalarm.domain.model.JourneyPlan
import kotlinx.coroutines.flow.Flow

interface JourneyPlanRepository {
    /** 영속화된 진행 중 여정 계획. 없으면 null. */
    suspend fun current(): JourneyPlan?

    /** UI(TrackingScreen 등)가 실시간 반영을 위해 관찰. */
    fun observeCurrent(): Flow<JourneyPlan?>

    suspend fun save(plan: JourneyPlan)

    suspend fun clear()
}
