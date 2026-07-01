package com.jin.subwayalarm.data.local

import com.jin.subwayalarm.domain.model.DirectionPolarity
import com.jin.subwayalarm.domain.model.LineMeta
import com.jin.subwayalarm.domain.model.LineTopology
import com.jin.subwayalarm.domain.model.Station

/**
 * 7호선 전체 53개 역 시드 데이터 (작업순서 3, 2026-07-01 실측 검증 완료).
 *
 * 검증 내역:
 *  - 역명·순서: `subwayStationMaster` API(openapi.seoul.go.kr:8088) 실응답으로 전량 확인.
 *  - statnId: `realtimePosition` 응답에서 11개 지점(장암·도봉산·중화·면목·용마산·건대입구·
 *    뚝섬유원지·강남구청·고속터미널·온수·석남)의 실제 값을 확인한 결과
 *    `statnId = 1007000709 + order`(장암=order 0) 패턴이 11/11 일치.
 *    나머지 역은 이 패턴으로 계산(추론) — 향후 정적 메타 동기화 API 연동 시 실측값으로 재검증 권장.
 *  - direction: 실시간 위치 44건 전수+ 3회 시계열(75초 간격, trainNo 39/40 유지 확인) 결과,
 *    `updnLine="1"` → 종착 온수/석남(남쪽) 방향, `updnLine="0"` → 종착 도봉산/장암(북쪽) 방향으로
 *    100% 일관. order 를 장암(0)→석남(52) 로 정의했으므로 order 증가(ASCENDING) = 석남 방향
 *    = updnLine "1".
 *  - trainSttus: 0(진입)·1(도착)·2(출발) 실측 확인(3=전역출발은 이번 샘플엔 미관측, 코드 자체는 유지).
 *
 * ⚠️ avgSecToNext 는 여전히 명목값(역간 소요시간 API 서비스명 미확정) — 보정 정밀도에는
 * 영향 있으나 "한 역 전 트리거"라는 목표엔 안전마진으로 흡수됨(설계 1-2/2-6).
 */
object SubwaySeed {

    const val LINE_7_ID = "1007"

    val line7Meta = LineMeta(
        lineId = LINE_7_ID,
        lineName = "7호선",
        supportsRealtime = true,
        topology = LineTopology.LINEAR,
        directionPolarity = DirectionPolarity(ascendingUpdnLine = "1"),
    )

    /** 장암(0) → 석남(52). 실측: subwayStationMaster 응답 그대로(괄호 병기역명 포함). */
    private val line7Names = listOf(
        "장암", "도봉산", "수락산", "마들", "노원", "중계", "하계", "공릉(서울과학기술대)",
        "태릉입구", "먹골", "중화", "상봉(시외버스터미널)", "면목", "사가정", "용마산", "중곡",
        "군자(능동)", "어린이대공원(세종대)", "건대입구", "뚝섬유원지", "청담", "강남구청", "학동",
        "논현", "반포", "고속터미널", "내방", "이수", "남성", "숭실대입구(살피재)", "상도",
        "장승배기", "신대방삼거리", "보라매", "신풍", "대림(구로구청)", "남구로", "가산디지털단지",
        "철산", "광명사거리", "천왕", "온수(성공회대입구)", "까치울", "부천종합운동장", "춘의",
        "신중동", "부천시청", "상동", "삼산체육관", "굴포천", "부평구청", "산곡", "석남(거북시장)",
    )

    /** statnId 산출 베이스(장암=709). 실측 11개 지점으로 검증된 패턴(주석 참조). */
    private const val STATN_ID_BASE = 709

    /** 명목 역간 소요(초). 실데이터로 교체 예정. 마지막 역(석남)은 0. */
    private const val NOMINAL_SEC = 120

    private fun statnId(order: Int): String = "%s%06d".format(LINE_7_ID, STATN_ID_BASE + order)

    val line7Stations: List<Station> = line7Names.mapIndexed { index, name ->
        Station(
            statnId = statnId(index),
            lineId = LINE_7_ID,
            name = name,
            order = index,
            avgSecToNext = if (index == line7Names.lastIndex) 0 else NOMINAL_SEC,
            nextStatnId = if (index == line7Names.lastIndex) null else statnId(index + 1),
            prevStatnId = if (index == 0) null else statnId(index - 1),
        )
    }
}
