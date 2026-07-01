package com.jin.subwayalarm.data.repository

import com.jin.subwayalarm.data.local.dao.JourneyPlanDao
import com.jin.subwayalarm.data.local.entity.JourneyPlanEntity
import com.jin.subwayalarm.domain.model.JourneyPlan
import com.jin.subwayalarm.domain.model.Leg
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JourneyPlanRepositoryImpl @Inject constructor(
    private val dao: JourneyPlanDao,
    moshi: Moshi,
) : JourneyPlanRepository {

    private val legsAdapter = moshi.adapter<List<Leg>>(
        Types.newParameterizedType(List::class.java, Leg::class.java),
    )

    override suspend fun current(): JourneyPlan? = dao.get()?.toDomain()

    override fun observeCurrent(): Flow<JourneyPlan?> = dao.observe().map { it?.toDomain() }

    override suspend fun save(plan: JourneyPlan) = dao.upsert(plan.toEntity())

    override suspend fun clear() = dao.clear()

    private fun JourneyPlanEntity.toDomain(): JourneyPlan? {
        val legs = runCatching { legsAdapter.fromJson(legsJson) }.getOrNull() ?: return null
        if (legs.isEmpty()) return null
        val safeIndex = currentLegIndex.coerceIn(0, legs.lastIndex)
        return JourneyPlan(legs = legs, currentLegIndex = safeIndex)
    }

    private fun JourneyPlan.toEntity(): JourneyPlanEntity =
        JourneyPlanEntity(legsJson = legsAdapter.toJson(legs), currentLegIndex = currentLegIndex)
}
