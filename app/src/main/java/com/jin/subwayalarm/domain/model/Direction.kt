package com.jin.subwayalarm.domain.model

/**
 * 진행 방향. 노선 정렬 순번(orderInLine) 기준으로 정의한다.
 *
 *  - ASCENDING : orderInLine 이 증가하는 방향으로 진행
 *  - DESCENDING: orderInLine 이 감소하는 방향으로 진행
 *
 * 실시간 API 의 updnLine(0/1) 과의 매핑 극성은 노선마다 다를 수 있어
 * 작업순서 3(실측 검증 스파이크)에서 노선별로 확정한다. 그래프 로직 자체는
 * updnLine 에 의존하지 않고 출발/목적 순번 비교로 방향을 도출한다.
 */
enum class Direction {
    ASCENDING,
    DESCENDING;

    /** 진행 방향의 순번 증분(+1 / -1). */
    val step: Int
        get() = if (this == ASCENDING) 1 else -1

    val opposite: Direction
        get() = if (this == ASCENDING) DESCENDING else ASCENDING
}
