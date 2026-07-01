package com.jin.subwayalarm.data.local

import com.jin.subwayalarm.domain.model.LineMeta
import com.jin.subwayalarm.domain.model.LineTopology
import com.jin.subwayalarm.domain.model.Station

/**
 * 7호선 시드 데이터 (작업순서 2, API 키 없이 진행하기 위한 임시 데이터).
 *
 * ⚠️ statnId 는 합성값("1007-NN")이고 avgSecToNext 는 명목값(약 90~120초)이다.
 * 작업순서 3(메타/실측 API)에서 실제 statnId·역간소요로 교체한다.
 * order 는 ASCENDING 방향 기준이며 updnLine 극성은 작업 3에서 확정.
 *
 * 구간: 7호선 강남~중랑 일부(고속터미널 → 용마산) 12개 역.
 */
object SubwaySeed {

    const val LINE_7_ID = "1007"

    val line7Meta = LineMeta(
        lineId = LINE_7_ID,
        lineName = "7호선",
        supportsRealtime = true,
        topology = LineTopology.LINEAR,
        // directionPolarity 는 실측 전(작업 3)이라 기본값 null 유지.
        // null 인 동안 MatchBoardedTrainUseCase 자동 매칭은 수행되지 않는다.
    )

    private val line7Names = listOf(
        "고속터미널", "반포", "논현", "학동", "강남구청", "청담",
        "뚝섬유원지", "건대입구", "어린이대공원", "군자", "중곡", "용마산",
    )

    /** 명목 역간 소요(초). 실데이터로 교체 예정. 마지막 역은 0. */
    private const val NOMINAL_SEC = 110

    val line7Stations: List<Station> = line7Names.mapIndexed { index, name ->
        val id = "%s-%02d".format(LINE_7_ID, index)
        Station(
            statnId = id,
            lineId = LINE_7_ID,
            name = name,
            order = index,
            avgSecToNext = if (index == line7Names.lastIndex) 0 else NOMINAL_SEC,
            nextStatnId = if (index == line7Names.lastIndex) null else "%s-%02d".format(LINE_7_ID, index + 1),
            prevStatnId = if (index == 0) null else "%s-%02d".format(LINE_7_ID, index - 1),
        )
    }
}
