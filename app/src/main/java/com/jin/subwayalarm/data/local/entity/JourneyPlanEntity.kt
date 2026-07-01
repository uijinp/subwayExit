package com.jin.subwayalarm.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 진행 중인 여정의 구간 계획 (사망 복구 SoT, TripSessionEntity 와 같은 단일행 패턴).
 * legsJson 은 List<Leg> 를 Moshi 로 직렬화한 문자열(JourneyPlanMapper 참조).
 */
@Entity(tableName = "journey_plan")
data class JourneyPlanEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val legsJson: String,
    val currentLegIndex: Int,
) {
    companion object {
        const val SINGLETON_ID = 0
    }
}
