package com.jin.subwayalarm.data.local

import com.jin.subwayalarm.domain.model.DirectionPolarity
import com.jin.subwayalarm.domain.model.LineMeta
import com.jin.subwayalarm.domain.model.LineTopology
import com.jin.subwayalarm.domain.model.Station

/**
 * 노선 시드 데이터 (실측 검증 완료분만 수록, LINEAR 노선 한정).
 *
 * 공통 검증 방법(2026-07-01):
 *  - 역명·순서·좌표: `subwayStationMaster` API 실응답 그대로.
 *  - statnId = subwayId + (statnIdBase + order). base 는 realtimePosition 실응답으로 확인.
 *  - directionPolarity(ascendingUpdnLine): 실측 열차의 updnLine↔종착역 대조로 확정.
 *    order 증가 방향(= 노선 끝역 방향)으로 가는 열차의 updnLine 값.
 *  - trainNo 안정성은 7호선에서 75초 간격 시계열로 검증(39/40 유지) — 동일 API 라 타 노선 동일 가정.
 *
 * 보류(추후): 2호선(순환+지선), 1·5·6·9호선(직결/분기/순환/ID불규칙), 8호선(statnId offset 불규칙).
 * ⚠️ avgSecToNext 는 명목값(120s) — "한 역 전 트리거" 목표엔 안전마진으로 흡수(설계 1-2/2-6).
 */
object SubwaySeed {

    data class RawStation(val name: String, val lat: Double, val lon: Double)

    data class LineSeed(val meta: LineMeta, val stations: List<Station>)

    private const val NOMINAL_SEC = 120

    private fun buildLine(
        lineId: String,
        lineName: String,
        ascendingUpdnLine: String,
        statnIdBase: Int,
        raws: List<RawStation>,
    ): LineSeed {
        fun statnId(order: Int): String = "%s%06d".format(lineId, statnIdBase + order)
        val stations = raws.mapIndexed { i, raw ->
            Station(
                statnId = statnId(i),
                lineId = lineId,
                name = raw.name,
                order = i,
                avgSecToNext = if (i == raws.lastIndex) 0 else NOMINAL_SEC,
                nextStatnId = if (i == raws.lastIndex) null else statnId(i + 1),
                prevStatnId = if (i == 0) null else statnId(i - 1),
                lat = raw.lat,
                lon = raw.lon,
            )
        }
        val meta = LineMeta(
            lineId = lineId,
            lineName = lineName,
            supportsRealtime = true,
            topology = LineTopology.LINEAR,
            directionPolarity = DirectionPolarity(ascendingUpdnLine),
        )
        return LineSeed(meta, stations)
    }

    /** 7호선: 장암(0) → 석남(52). base=709, ascending=updnLine "1"(석남 방향). 실측 44건+시계열 검증. */
    val line7: LineSeed = buildLine(
        "1007", "7호선", "1", 709,
        listOf(
            RawStation("장암", 37.700109, 127.053196),
            RawStation("도봉산", 37.689241, 127.046509),
            RawStation("수락산", 37.67785, 127.055315),
            RawStation("마들", 37.66494, 127.057675),
            RawStation("노원", 37.654836, 127.060462),
            RawStation("중계", 37.644583, 127.064303),
            RawStation("하계", 37.636352, 127.06799),
            RawStation("공릉(서울과학기술대)", 37.625742, 127.072896),
            RawStation("태릉입구", 37.618294, 127.075397),
            RawStation("먹골", 37.610637, 127.077725),
            RawStation("중화", 37.602545, 127.079264),
            RawStation("상봉(시외버스터미널)", 37.595577, 127.085716),
            RawStation("면목", 37.588579, 127.087503),
            RawStation("사가정", 37.580894, 127.088478),
            RawStation("용마산", 37.573647, 127.086727),
            RawStation("중곡", 37.565923, 127.08432),
            RawStation("군자(능동)", 37.556897, 127.079338),
            RawStation("어린이대공원(세종대)", 37.548014, 127.074658),
            RawStation("건대입구", 37.540786, 127.071011),
            RawStation("뚝섬유원지", 37.53154, 127.066704),
            RawStation("청담", 37.519365, 127.05335),
            RawStation("강남구청", 37.517179, 127.041255),
            RawStation("학동", 37.514229, 127.031656),
            RawStation("논현", 37.511093, 127.021415),
            RawStation("반포", 37.508178, 127.011727),
            RawStation("고속터미널", 37.503367, 127.005068),
            RawStation("내방", 37.487618, 126.993513),
            RawStation("이수", 37.485196, 126.981605),
            RawStation("남성", 37.484596, 126.971251),
            RawStation("숭실대입구(살피재)", 37.496029, 126.953822),
            RawStation("상도", 37.502834, 126.94791),
            RawStation("장승배기", 37.504898, 126.93915),
            RawStation("신대방삼거리", 37.499701, 126.928276),
            RawStation("보라매", 37.499872, 126.920428),
            RawStation("신풍", 37.50008, 126.90993),
            RawStation("대림(구로구청)", 37.493013, 126.897075),
            RawStation("남구로", 37.486056, 126.887249),
            RawStation("가산디지털단지", 37.480338, 126.882656),
            RawStation("철산", 37.47605, 126.867911),
            RawStation("광명사거리", 37.479252, 126.854876),
            RawStation("천왕", 37.486637, 126.838713),
            RawStation("온수(성공회대입구)", 37.492092, 126.823023),
            RawStation("까치울", 37.506207, 126.810939),
            RawStation("부천종합운동장", 37.50538, 126.797337),
            RawStation("춘의", 37.503663, 126.787036),
            RawStation("신중동", 37.503048, 126.77596),
            RawStation("부천시청", 37.504631, 126.763538),
            RawStation("상동", 37.505781, 126.753083),
            RawStation("삼산체육관", 37.506411, 126.742153),
            RawStation("굴포천", 37.506997, 126.73128),
            RawStation("부평구청", 37.507394, 126.721599),
            RawStation("산곡", 37.5086, 126.7035277),
            RawStation("석남(거북시장)", 37.5062285, 126.6762813),
        ),
    )

    /** 3호선: 지축(0) → 오금(33). base=319, ascending=updnLine "1"(오금 방향). 실측 offset 319 일치. */
    val line3: LineSeed = buildLine(
        "1003", "3호선", "1", 319,
        listOf(
            RawStation("지축", 37.648033, 126.913917),
            RawStation("구파발", 37.636763, 126.918821),
            RawStation("연신내", 37.619229, 126.921038),
            RawStation("불광", 37.610553, 126.92982),
            RawStation("녹번", 37.600927, 126.935756),
            RawStation("홍제", 37.589066, 126.943736),
            RawStation("무악재", 37.582299, 126.950291),
            RawStation("독립문", 37.574571, 126.957748),
            RawStation("경복궁(정부서울청사)", 37.575762, 126.97353),
            RawStation("안국", 37.576477, 126.985443),
            RawStation("종로3가", 37.571605, 126.991791),
            RawStation("을지로3가", 37.566672, 126.992548),
            RawStation("충무로", 37.56143, 126.994072),
            RawStation("동대입구", 37.559052, 127.005602),
            RawStation("약수", 37.554867, 127.010541),
            RawStation("금호", 37.548034, 127.015872),
            RawStation("옥수", 37.541684, 127.017269),
            RawStation("압구정", 37.527072, 127.028461),
            RawStation("신사", 37.516334, 127.020114),
            RawStation("잠원", 37.512759, 127.01122),
            RawStation("고속터미널", 37.504891, 127.004916),
            RawStation("교대(법원.검찰청)", 37.493025, 127.013786),
            RawStation("남부터미널(예술의전당)", 37.485013, 127.016189),
            RawStation("양재(서초구청)", 37.484477, 127.033902),
            RawStation("매봉", 37.486947, 127.046769),
            RawStation("도곡", 37.490922, 127.055452),
            RawStation("대치", 37.494612, 127.063642),
            RawStation("학여울", 37.496663, 127.070594),
            RawStation("대청", 37.493514, 127.079532),
            RawStation("일원", 37.483681, 127.08439),
            RawStation("수서", 37.487378, 127.101907),
            RawStation("가락시장", 37.492245, 127.117757),
            RawStation("경찰병원", 37.495918, 127.12454),
            RawStation("오금", 37.502129, 127.128319),
        ),
    )

    /** 4호선: 불암산(0) → 남태령(25). base=409, ascending=updnLine "1"(남태령/오이도 방향). 실측 offset 409 일치. */
    val line4: LineSeed = buildLine(
        "1004", "4호선", "1", 409,
        listOf(
            RawStation("불암산", 37.670272, 127.079066),
            RawStation("상계", 37.660878, 127.073572),
            RawStation("노원", 37.65627, 127.063276),
            RawStation("창동", 37.653088, 127.047274),
            RawStation("쌍문", 37.648627, 127.034709),
            RawStation("수유(강북구청)", 37.638052, 127.025732),
            RawStation("미아(서울사이버대학)", 37.62667, 127.025983),
            RawStation("미아사거리", 37.613292, 127.030053),
            RawStation("길음", 37.603407, 127.025053),
            RawStation("성신여대입구(돈암)", 37.592612, 127.016441),
            RawStation("한성대입구(삼선교)", 37.588458, 127.006221),
            RawStation("혜화", 37.582336, 127.001844),
            RawStation("동대문", 37.57093, 127.009287),
            RawStation("동대문역사문화공원", 37.565133, 127.007885),
            RawStation("충무로", 37.561207, 126.99408),
            RawStation("명동", 37.560989, 126.986325),
            RawStation("회현(남대문시장)", 37.558514, 126.978246),
            RawStation("서울역", 37.55281, 126.972556),
            RawStation("숙대입구(갈월)", 37.54456, 126.972106),
            RawStation("삼각지", 37.534075, 126.9726),
            RawStation("신용산", 37.52917, 126.967894),
            RawStation("이촌(국립중앙박물관)", 37.522295, 126.974733),
            RawStation("동작(현충원)", 37.502852, 126.980347),
            RawStation("총신대입구(이수)", 37.486263, 126.981989),
            RawStation("사당", 37.476955, 126.981651),
            RawStation("남태령", 37.463873, 126.989134),
        ),
    )

    /**
     * 6호선: 응암(0) → 신내(38). base=610, ascending=updnLine "1"(신내 방향). 실측 offset 610 전수 일치.
     * ⚠️ 응암 인근(응암·역촌·불광·독바위·연신내·구산·새절)은 실제로 단선 순환(응암순환)이라
     * 이 구간을 포함하는 여정은 "한 역 전" 타이밍이 어긋날 수 있음(근사). 본선 구간은 정상.
     */
    val line6: LineSeed = buildLine(
        "1006", "6호선", "1", 610,
        listOf(
            RawStation("응암", 37.598605, 126.915577),
            RawStation("역촌", 37.606021, 126.922744),
            RawStation("불광", 37.610873, 126.92939),
            RawStation("독바위", 37.618456, 126.933031),
            RawStation("연신내", 37.618636, 126.920625),
            RawStation("구산", 37.611377, 126.91727),
            RawStation("새절(신사)", 37.591148, 126.913629),
            RawStation("증산(명지대앞)", 37.583876, 126.909645),
            RawStation("디지털미디어시티", 37.576108, 126.901391),
            RawStation("월드컵경기장(성산)", 37.569532, 126.899298),
            RawStation("마포구청", 37.563515, 126.903343),
            RawStation("망원", 37.556094, 126.910052),
            RawStation("합정", 37.549209, 126.913366),
            RawStation("상수", 37.547716, 126.922852),
            RawStation("광흥창(서강)", 37.547456, 126.931993),
            RawStation("대흥(서강대앞)", 37.547771, 126.942069),
            RawStation("공덕", 37.543555, 126.951678),
            RawStation("효창공원앞", 37.539233, 126.961384),
            RawStation("삼각지", 37.535534, 126.974032),
            RawStation("녹사평(용산구청)", 37.534675, 126.986695),
            RawStation("이태원", 37.534488, 126.994302),
            RawStation("한강진", 37.539631, 127.001725),
            RawStation("버티고개", 37.548013, 127.007055),
            RawStation("약수", 37.554263, 127.010358),
            RawStation("청구", 37.560608, 127.013986),
            RawStation("신당", 37.566154, 127.016146),
            RawStation("동묘앞", 37.572279, 127.015653),
            RawStation("창신", 37.579661, 127.015241),
            RawStation("보문", 37.585274, 127.019351),
            RawStation("안암(고대병원앞)", 37.586272, 127.029005),
            RawStation("고려대(종암)", 37.590508, 127.036296),
            RawStation("월곡(동덕여대)", 37.601948, 127.041518),
            RawStation("상월곡(한국과학기술연구원)", 37.606377, 127.048491),
            RawStation("돌곶이", 37.610537, 127.056431),
            RawStation("석계", 37.614872, 127.065595),
            RawStation("태릉입구", 37.617338, 127.074735),
            RawStation("화랑대(서울여대입구)", 37.620064, 127.084689),
            RawStation("봉화산(서울의료원)", 37.617283, 127.091401),
            RawStation("신내", 37.613174, 127.102231),
        ),
    )

    /** 9호선: 개화(0) → 중앙보훈병원(37). base=901, ascending=updnLine "1"(중앙보훈병원 방향).
     *  마스터의 "9호선"+"9호선(연장)"을 BLDN_ID 순으로 병합. 실측 offset 901 전수 일치. */
    val line9: LineSeed = buildLine(
        "1009", "9호선", "1", 901,
        listOf(
            RawStation("개화", 37.578608, 126.798153),
            RawStation("김포공항", 37.561916, 126.802152),
            RawStation("공항시장", 37.563726, 126.810678),
            RawStation("신방화", 37.567532, 126.816601),
            RawStation("마곡나루", 37.566778, 126.82731),
            RawStation("양천향교", 37.568381, 126.841333),
            RawStation("가양", 37.561391, 126.854456),
            RawStation("증미", 37.557402, 126.861939),
            RawStation("등촌", 37.550632, 126.865689),
            RawStation("염창", 37.546936, 126.874916),
            RawStation("신목동", 37.544277, 126.88308),
            RawStation("선유도", 37.53802, 126.893525),
            RawStation("당산", 37.533406, 126.902809),
            RawStation("국회의사당", 37.528105, 126.917874),
            RawStation("여의도", 37.52176, 126.92403),
            RawStation("샛강", 37.517274, 126.928422),
            RawStation("노량진", 37.513534, 126.941005),
            RawStation("노들", 37.512887, 126.953222),
            RawStation("흑석(중앙대입구)", 37.50877, 126.963708),
            RawStation("동작(현충원)", 37.502878, 126.978153),
            RawStation("구반포", 37.501364, 126.987332),
            RawStation("신반포", 37.503415, 126.995925),
            RawStation("고속터미널", 37.50598, 127.004403),
            RawStation("사평", 37.504206, 127.015259),
            RawStation("신논현", 37.504598, 127.02506),
            RawStation("언주", 37.507287, 127.033868),
            RawStation("선정릉", 37.510297, 127.043999),
            RawStation("삼성중앙", 37.513011, 127.053282),
            RawStation("봉은사", 37.514219, 127.060245),
            RawStation("종합운동장", 37.511426, 127.076275),
            RawStation("삼전", 37.504738, 127.088025),
            RawStation("석촌고분", 37.502558, 127.097033),
            RawStation("석촌", 37.505208, 127.10704),
            RawStation("송파나루", 37.510372, 127.112216),
            RawStation("한성백제", 37.516404, 127.116503),
            RawStation("올림픽공원(한국체대)", 37.516269, 127.130288),
            RawStation("둔촌오륜", 37.519683, 127.137989),
            RawStation("중앙보훈병원", 37.529191, 127.148739),
        ),
    )

    /**
     * 1호선(경인선 구간): 청량리(0) → 인천(37). base=124, ascending=updnLine "1"(인천 방향).
     * 수집기 시계열 인접관계 + statnId 연속(인천 161 → 청량리 124) 실측으로 순서 검증(34/38 전수 일치).
     *
     * ⚠️ "1호선"은 실제로 구로에서 경부선(수원·천안)과 갈라지는 분기망이지만, 실시간 API 가
     * 전 구간을 subwayId "1001"로 반환하므로 노선 매칭상 lineId="1001" 항목은 하나만 둘 수 있다.
     * 사용자 요청(인천·부평 ↔ 서울역·청량리)에 맞춰 경인 구간만 수록. 경부/경원 구간은 미포함.
     * 트렁크역(구로~청량리)은 경부 열차와 공유되므로 그 구간에서 "탑승"하면 매칭이 모호할 수 있음
     * (인천·부평 등 경인 전용역에서 타면 정상). 탑승 확정 후 추적은 boardedTrainNo 기준이라 정상.
     */
    val line1Gyeongin: LineSeed = buildLine(
        "1001", "1호선", "1", 124,
        listOf(
            RawStation("청량리(서울시립대입구)", 37.579956, 127.044585),
            RawStation("제기동", 37.578103, 127.034893),
            RawStation("신설동", 37.576048, 127.024634),
            RawStation("동묘앞", 37.573197, 127.01648),
            RawStation("동대문", 37.571687, 127.01093),
            RawStation("종로5가", 37.570926, 127.001849),
            RawStation("종로3가", 37.570406, 126.991847),
            RawStation("종각", 37.570161, 126.982923),
            RawStation("시청", 37.565715, 126.977088),
            RawStation("서울역", 37.556228, 126.972135),
            RawStation("남영", 37.541021, 126.9713),
            RawStation("용산", 37.529849, 126.964561),
            RawStation("노량진", 37.514149, 126.94271),
            RawStation("대방", 37.513342, 126.926382),
            RawStation("신길", 37.516862, 126.917865),
            RawStation("영등포", 37.515504, 126.907628),
            RawStation("신도림", 37.508961, 126.891084),
            RawStation("구로", 37.503039, 126.881966),
            RawStation("구일", 37.496756, 126.870793),
            RawStation("개봉", 37.494594, 126.85968),
            RawStation("오류동", 37.494526, 126.845365),
            RawStation("온수(성공회대입구)", 37.492433, 126.824086),
            RawStation("역곡", 37.485178, 126.811502),
            RawStation("소사", 37.482753, 126.79544),
            RawStation("부천", 37.48405, 126.782686),
            RawStation("중동", 37.486562, 126.764843),
            RawStation("송내", 37.4876, 126.753664),
            RawStation("부개", 37.488418, 126.74109),
            RawStation("부평", 37.489445, 126.724506),
            RawStation("백운", 37.483664, 126.707704),
            RawStation("동암", 37.471408, 126.702896),
            RawStation("간석", 37.464737, 126.694181),
            RawStation("주안", 37.465047, 126.679742),
            RawStation("도화", 37.46607, 126.668672),
            RawStation("제물포", 37.466769, 126.656666),
            RawStation("도원", 37.468446, 126.642706),
            RawStation("동인천", 37.475276, 126.632802),
            RawStation("인천", 37.476079, 126.616801),
        ),
    )

    val allLines: List<LineSeed> = listOf(line1Gyeongin, line3, line4, line6, line7, line9)

    // --- 하위호환(기존 테스트·StationRepositoryImpl 참조) ---
    const val LINE_7_ID = "1007"
    val line7Meta: LineMeta get() = line7.meta
    val line7Stations: List<Station> get() = line7.stations
}
