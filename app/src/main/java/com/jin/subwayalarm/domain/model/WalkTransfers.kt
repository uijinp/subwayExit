package com.jin.subwayalarm.domain.model

/**
 * 역명 표기가 달라 exact-name 환승이 안 되는 동일 환승역 쌍 (보행 환승).
 * SubwayNetwork 가 neighbors 에 별칭 엣지를 추가한다 (DATA_AUDIT D-1).
 */
object WalkTransfers {

    /** (이름A, 이름B) — 양방향. */
    val pairs: List<Pair<String, String>> = listOf(
        "이수" to "총신대입구(이수)",
    )

    /** 역명 → 보행 환승으로 이어지는 다른 표기들. */
    val aliasesOf: Map<String, Set<String>> = run {
        val map = mutableMapOf<String, MutableSet<String>>()
        for ((a, b) in pairs) {
            map.getOrPut(a) { mutableSetOf() }.add(b)
            map.getOrPut(b) { mutableSetOf() }.add(a)
        }
        map.mapValues { it.value.toSet() }
    }

    fun aliases(name: String): Set<String> = aliasesOf[name].orEmpty()
}
