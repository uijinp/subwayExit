package com.jin.subwayalarm.domain.model

/**
 * 노선의 updnLine(API 원본, "0"/"1") 값 ↔ [Direction] 매핑.
 *
 * 극성은 노선마다 다를 수 있어(2-2 주석 참조) 작업순서 3(실측 검증 스파이크)에서
 * 노선별로 실측 후 확정한다. [LineMeta.directionPolarity] 가 null 이면 아직 미검증 —
 * 이 경우 [com.jin.subwayalarm.domain.usecase.MatchBoardedTrainUseCase] 자동 매칭을
 * 수행하지 않고 수동 확정을 기다린다(그래프 자체의 방향 판정은 1-6 에 따라
 * updnLine 에 의존하지 않으므로 영향받지 않음).
 */
data class DirectionPolarity(val ascendingUpdnLine: String) {
    fun directionOf(updnLine: String): Direction? = when {
        updnLine == ascendingUpdnLine -> Direction.ASCENDING
        updnLine.isNotBlank() -> Direction.DESCENDING
        else -> null
    }
}
