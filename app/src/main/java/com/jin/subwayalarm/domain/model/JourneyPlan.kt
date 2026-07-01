package com.jin.subwayalarm.domain.model

/**
 * 진행 중인 여정의 전체 계획(구간 목록 + 현재 몇 번째 구간인지).
 * 환승 없는 단일 노선 여정도 legs.size==1 인 JourneyPlan 으로 통일해서 다룬다.
 */
data class JourneyPlan(
    val legs: List<Leg>,
    val currentLegIndex: Int = 0,
) {
    init {
        require(legs.isNotEmpty()) { "legs 가 비어 있습니다." }
        require(currentLegIndex in legs.indices) { "currentLegIndex 범위 초과: $currentLegIndex (size=${legs.size})" }
    }

    val currentLeg: Leg get() = legs[currentLegIndex]
    val isLastLeg: Boolean get() = currentLegIndex == legs.lastIndex
    val hasNextLeg: Boolean get() = !isLastLeg
    val totalLegs: Int get() = legs.size
}
