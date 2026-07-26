package com.jin.subwayalarm.domain.usecase

import com.jin.subwayalarm.domain.model.Station
import com.jin.subwayalarm.domain.model.TrainPosition
import javax.inject.Inject

/**
 * 실시간 응답의 `statnId` 를 시드 기준 ID 로 정규화 (순수 로직).
 *
 * 광역선 시드의 statnId 는 공식으로 생성돼 있어 실제 API 값과 어긋난 역이 있다
 * (실측 2026-07-16: 경의중앙선 옥수 시드 `1063000114` ↔ API `1063075114`,
 * 수인분당선 개포동 `…000219` ↔ `…075219`, 경춘선 신내 `…000122` ↔ `…080122`,
 * 우이신설선·신분당선 일부). 정규화하지 않으면 그 역에 열차가 있는 동안
 * `graph.station()` 이 null 이라 위치를 못 잡고, `EvaluateAlert` 가 Invalid 를 내며
 * **추적이 그대로 멈춘다.**
 *
 * 역명은 노선 안에서 유일하므로(`NormalizePositionsUseCaseTest` 가 전 노선 검증)
 * statnId 로 못 찾으면 역명으로 찾아 시드 ID 로 바꿔준다. 둘 다 못 찾으면
 * (시드 구간 밖 열차 등) 원본을 그대로 두고 하위 로직이 자연히 걸러내게 한다.
 *
 * 폴링 응답이 도메인에 들어오는 **경계에서 한 번만** 적용해, 하위 판정 로직
 * (`MatchBoardedTrain`·`RecptnDtCorrector`·`EvaluateAlert`)은 손대지 않는다.
 */
class NormalizePositionsUseCase @Inject constructor() {

    operator fun invoke(positions: List<TrainPosition>, stations: List<Station>): List<TrainPosition> {
        if (positions.isEmpty() || stations.isEmpty()) return positions
        val knownIds = stations.mapTo(HashSet()) { it.statnId }
        val idByName = stations.associate { it.name to it.statnId }

        // 3차 폴백: 괄호 부기·공백만 다른 표기 (시드 "청량리(서울시립대입구)" ↔ API "청량리",
        // "4.19민주묘지" ↔ "4.19 민주묘지"). 정규화 후 충돌하는 키는 어느 역인지 확정할 수 없어 제외한다.
        val idByLooseName = stations
            .groupBy { looseKey(it.name) }
            .filterValues { it.size == 1 }
            .mapValues { (_, group) -> group.single().statnId }

        return positions.map { position ->
            if (position.statnId in knownIds) {
                position
            } else {
                val resolved = idByName[position.statnName]
                    ?: idByLooseName[looseKey(position.statnName)]
                if (resolved == null) position else position.copy(statnId = resolved)
            }
        }
    }

    /**
     * 괄호 부기·공백·끝의 "역" 을 제거한 비교용 키.
     * API 가 서울역을 "서울" 로 주는 등 표기가 흔들려서, 끝 글자 "역" 도 떼고 비교한다.
     */
    private fun looseKey(name: String): String {
        val base = name.replace(PAREN, "").replace(" ", "")
        return if (base.length > 1 && base.endsWith("역")) base.dropLast(1) else base
    }

    private companion object {
        val PAREN = Regex("\\([^)]*\\)")
    }
}
