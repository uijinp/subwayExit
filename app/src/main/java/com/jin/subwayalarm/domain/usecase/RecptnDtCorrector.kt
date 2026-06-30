package com.jin.subwayalarm.domain.usecase

import com.jin.subwayalarm.domain.model.Direction
import com.jin.subwayalarm.domain.model.StationGraph

/**
 * recptnDt 보정 결과. [stationsAdvanced] 는 로깅·모드 인디케이터용 부가 정보.
 */
data class CorrectedPosition(
    val statnId: String,
    val stationsAdvanced: Int,
)

/**
 * recptnDt 시차(lag) 보정 (설계 2-6).
 *
 * 상수 임계치("1분30초→1역", v1) 대신, 역간 평균 소요시간(avgSecToNext)으로
 * 세그먼트마다 나눠서 "그 구간의 50% 이상을 이미 지났을 시간"이면 한 역 진행한 것으로 본다.
 * → 작은 lag(네트워크 지연 수준)는 보정하지 않고, 폴링 공백처럼 큰 lag만 보정한다.
 *
 * 규칙(1-2): 항상 "더 빠른 알람" 방향으로만 반올림한다 — 늦느니 한 역 일찍.
 * round-half-up(임계 0.5)이 이를 충족: 50% 미만이면 아직 안 지난 것으로 간주하지만,
 * 정확히 50%부터는 이미 지난 것으로 간주해 조기 쪽으로 기운다.
 */
object RecptnDtCorrector {

    private const val ADVANCE_THRESHOLD = 0.5

    fun correct(
        graph: StationGraph,
        rawStatnId: String,
        direction: Direction,
        recptnDtEpochMillis: Long,
        nowEpochMillis: Long,
    ): CorrectedPosition {
        val station = graph.station(rawStatnId)
            ?: return CorrectedPosition(rawStatnId, 0) // 그래프 밖 역(예: 타 노선 표기) → 보정 불가, 원본 유지

        val lagSeconds = (nowEpochMillis - recptnDtEpochMillis) / 1000.0
        if (lagSeconds <= 0.0) return CorrectedPosition(station.statnId, 0)

        var current = station
        var advanced = 0
        var remainingLag = lagSeconds

        while (true) {
            val next = graph.nextStation(current.statnId, direction) ?: break
            // avgSecToNext 는 "더 낮은 순번 역"에 저장된 구간 비용이다.
            // ASCENDING 이면 current 가 낮은 순번, DESCENDING 이면 next 가 낮은 순번.
            val segmentCost = if (direction == Direction.ASCENDING) current.avgSecToNext else next.avgSecToNext
            if (segmentCost <= 0) break // 구간 정보 없음(종착 등) — 더 진행 불가
            if (remainingLag / segmentCost < ADVANCE_THRESHOLD) break
            current = next
            advanced++
            remainingLag -= segmentCost
        }

        return CorrectedPosition(current.statnId, advanced)
    }
}
