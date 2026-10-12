package com.jin.subwayalarm.data.local

import com.jin.subwayalarm.domain.model.DirectionPolarity
import com.jin.subwayalarm.domain.model.LineMeta
import com.jin.subwayalarm.domain.model.LineTopology
import com.jin.subwayalarm.domain.model.Station

/**
 * 수도권 추가 노선 시드 (2·5·8 + 광역). SearchSTN 순서 + FR→statnId 공식 + 실시간 exact 매핑.
 *
 * ⚠️ order 는 반드시 **지리적 진행 순서**여야 한다. order/edges 가 어긋나면 노선이 도시를
 * 가로질러 이어진 것처럼 되어 환승 경로가 엉뚱해지고 추적 시 직전역 판정이 전부 틀린다.
 * `SeedAdjacencyDistanceTest` 가 인접역 8km 규칙으로 이를 자동 검증한다.
 *
 * directionPolarity 실측 검증 (2026-07-16, realtimePosition 의 updnLine × statnTnm 대조):
 *  - 신분당선  1077: updnLine=1 → 종착 광교(order 15, 최대)          ⇒ ascending="1"
 *  - 공항철도  1065: updnLine=1 → 종착 인천공항2터미널(order 13, 최대) ⇒ ascending="1"
 *  - 수인분당선 1075: updnLine=1 → 종착 인천(62)·고색(37)·죽전(24)     ⇒ ascending="1"
 *  - 경의중앙선 1063: updnLine=1 → 종착 용문(53)·덕소(42)·서울(56)     ⇒ ascending="1"
 *    (updnLine=0 은 각각 신사·서울·왕십리/청량리·문산/대곡 = order 작은 쪽)
 */
object ExtraLinesSeed {
    private const val SEC = 120

    private val stations_1002: List<Station> = listOf(
        Station(statnId="1002000201", lineId="1002", name="시청", order=0, avgSecToNext=SEC, lat=37.563588, lon=126.975411),
        Station(statnId="1002000202", lineId="1002", name="을지로입구", order=1, avgSecToNext=SEC, lat=37.566014, lon=126.982618),
        Station(statnId="1002000203", lineId="1002", name="을지로3가", order=2, avgSecToNext=SEC, lat=37.566306, lon=126.991696),
        Station(statnId="1002000204", lineId="1002", name="을지로4가", order=3, avgSecToNext=SEC, lat=37.566595, lon=126.997817),
        Station(statnId="1002000205", lineId="1002", name="동대문역사문화공원", order=4, avgSecToNext=SEC, lat=37.565613, lon=127.009054),
        Station(statnId="1002000206", lineId="1002", name="신당", order=5, avgSecToNext=SEC, lat=37.56564, lon=127.019614),
        Station(statnId="1002000207", lineId="1002", name="상왕십리", order=6, avgSecToNext=SEC, lat=37.564354, lon=127.029354),
        Station(statnId="1002000208", lineId="1002", name="왕십리", order=7, avgSecToNext=SEC, lat=37.561238, lon=127.036954),
        Station(statnId="1002000209", lineId="1002", name="한양대", order=8, avgSecToNext=SEC, lat=37.555273, lon=127.043655),
        Station(statnId="1002000210", lineId="1002", name="뚝섬", order=9, avgSecToNext=SEC, lat=37.547184, lon=127.047367),
        Station(statnId="1002000211", lineId="1002", name="성수", order=10, avgSecToNext=SEC, lat=37.544581, lon=127.055961),
        Station(statnId="1002000212", lineId="1002", name="건대입구", order=11, avgSecToNext=SEC, lat=37.540373, lon=127.069191),
        Station(statnId="1002000213", lineId="1002", name="구의", order=12, avgSecToNext=SEC, lat=37.537077, lon=127.085916),
        Station(statnId="1002000214", lineId="1002", name="강변", order=13, avgSecToNext=SEC, lat=37.535095, lon=127.094681),
        Station(statnId="1002000215", lineId="1002", name="잠실나루", order=14, avgSecToNext=SEC, lat=37.520733, lon=127.10379),
        Station(statnId="1002000216", lineId="1002", name="잠실", order=15, avgSecToNext=SEC, lat=37.513262, lon=127.100159),
        Station(statnId="1002000217", lineId="1002", name="잠실새내", order=16, avgSecToNext=SEC, lat=37.511687, lon=127.086162),
        Station(statnId="1002000218", lineId="1002", name="종합운동장", order=17, avgSecToNext=SEC, lat=37.511022, lon=127.073704),
        Station(statnId="1002000219", lineId="1002", name="삼성", order=18, avgSecToNext=SEC, lat=37.508844, lon=127.06316),
        Station(statnId="1002000220", lineId="1002", name="선릉", order=19, avgSecToNext=SEC, lat=37.504286, lon=127.048203),
        Station(statnId="1002000221", lineId="1002", name="역삼", order=20, avgSecToNext=SEC, lat=37.500622, lon=127.036456),
        Station(statnId="1002000222", lineId="1002", name="강남", order=21, avgSecToNext=SEC, lat=37.49799, lon=127.027912),
        Station(statnId="1002000223", lineId="1002", name="교대(법원.검찰청)", order=22, avgSecToNext=SEC, lat=37.493961, lon=127.014667),
        Station(statnId="1002000224", lineId="1002", name="서초", order=23, avgSecToNext=SEC, lat=37.491897, lon=127.007917),
        Station(statnId="1002000225", lineId="1002", name="방배", order=24, avgSecToNext=SEC, lat=37.481426, lon=126.997596),
        Station(statnId="1002000226", lineId="1002", name="사당", order=25, avgSecToNext=SEC, lat=37.476538, lon=126.981544),
        Station(statnId="1002000227", lineId="1002", name="낙성대", order=26, avgSecToNext=SEC, lat=37.47693, lon=126.963693),
        Station(statnId="1002000228", lineId="1002", name="서울대입구", order=27, avgSecToNext=SEC, lat=37.481247, lon=126.952739),
        Station(statnId="1002000229", lineId="1002", name="봉천", order=28, avgSecToNext=SEC, lat=37.482362, lon=126.941892),
        Station(statnId="1002000230", lineId="1002", name="신림", order=29, avgSecToNext=SEC, lat=37.484201, lon=126.929715),
        Station(statnId="1002000231", lineId="1002", name="신대방", order=30, avgSecToNext=SEC, lat=37.487462, lon=126.913149),
        Station(statnId="1002000232", lineId="1002", name="구로디지털단지", order=31, avgSecToNext=SEC, lat=37.485266, lon=126.901401),
        Station(statnId="1002000233", lineId="1002", name="대림(구로구청)", order=32, avgSecToNext=SEC, lat=37.493243, lon=126.894932),
        Station(statnId="1002000234", lineId="1002", name="신도림", order=33, avgSecToNext=SEC, lat=37.508961, lon=126.891084),
        Station(statnId="1002000235", lineId="1002", name="문래", order=34, avgSecToNext=SEC, lat=37.517933, lon=126.89476),
        Station(statnId="1002000236", lineId="1002", name="영등포구청", order=35, avgSecToNext=SEC, lat=37.525706, lon=126.89661),
        Station(statnId="1002000237", lineId="1002", name="당산", order=36, avgSecToNext=SEC, lat=37.534946, lon=126.902767),
        Station(statnId="1002000238", lineId="1002", name="합정", order=37, avgSecToNext=SEC, lat=37.549457, lon=126.913808),
        Station(statnId="1002000239", lineId="1002", name="홍대입구", order=38, avgSecToNext=SEC, lat=37.55679, lon=126.923708),
        Station(statnId="1002000240", lineId="1002", name="신촌", order=39, avgSecToNext=SEC, lat=37.555131, lon=126.936926),
        Station(statnId="1002000241", lineId="1002", name="이대", order=40, avgSecToNext=SEC, lat=37.556733, lon=126.946013),
        Station(statnId="1002000242", lineId="1002", name="아현", order=41, avgSecToNext=SEC, lat=37.557345, lon=126.956141),
        Station(statnId="1002000243", lineId="1002", name="충정로", order=42, avgSecToNext=SEC, lat=37.559704, lon=126.964378),
        Station(statnId="1002002111", lineId="1002", name="용답", order=43, avgSecToNext=SEC, lat=37.561904, lon=127.050899),
        Station(statnId="1002002112", lineId="1002", name="신답", order=44, avgSecToNext=SEC, lat=37.57004, lon=127.046481),
        Station(statnId="1002002113", lineId="1002", name="용두", order=45, avgSecToNext=SEC, lat=37.574028, lon=127.038091),
        Station(statnId="1002002114", lineId="1002", name="신설동", order=46, avgSecToNext=SEC, lat=37.574747, lon=127.024932),
        Station(statnId="1002002341", lineId="1002", name="도림천", order=47, avgSecToNext=SEC, lat=37.514287, lon=126.882768),
        Station(statnId="1002002342", lineId="1002", name="양천구청", order=48, avgSecToNext=SEC, lat=37.512398, lon=126.865819),
        Station(statnId="1002002343", lineId="1002", name="신정네거리", order=49, avgSecToNext=SEC, lat=37.520074, lon=126.852912),
        Station(statnId="1002002344", lineId="1002", name="까치산", order=50, avgSecToNext=SEC, lat=37.531768, lon=126.846683),
    )
    private val edges_1002: List<Pair<String, String>> = listOf(
        "1002000201" to "1002000202",
        "1002000202" to "1002000203",
        "1002000203" to "1002000204",
        "1002000204" to "1002000205",
        "1002000205" to "1002000206",
        "1002000206" to "1002000207",
        "1002000207" to "1002000208",
        "1002000208" to "1002000209",
        "1002000209" to "1002000210",
        "1002000210" to "1002000211",
        "1002000211" to "1002000212",
        "1002000212" to "1002000213",
        "1002000213" to "1002000214",
        "1002000214" to "1002000215",
        "1002000215" to "1002000216",
        "1002000216" to "1002000217",
        "1002000217" to "1002000218",
        "1002000218" to "1002000219",
        "1002000219" to "1002000220",
        "1002000220" to "1002000221",
        "1002000221" to "1002000222",
        "1002000222" to "1002000223",
        "1002000223" to "1002000224",
        "1002000224" to "1002000225",
        "1002000225" to "1002000226",
        "1002000226" to "1002000227",
        "1002000227" to "1002000228",
        "1002000228" to "1002000229",
        "1002000229" to "1002000230",
        "1002000230" to "1002000231",
        "1002000231" to "1002000232",
        "1002000232" to "1002000233",
        "1002000233" to "1002000234",
        "1002000234" to "1002000235",
        "1002000235" to "1002000236",
        "1002000236" to "1002000237",
        "1002000237" to "1002000238",
        "1002000238" to "1002000239",
        "1002000239" to "1002000240",
        "1002000240" to "1002000241",
        "1002000241" to "1002000242",
        "1002000242" to "1002000243",
        "1002000243" to "1002000201",
        "1002000211" to "1002002111",
        "1002002111" to "1002002112",
        "1002002112" to "1002002113",
        "1002002113" to "1002002114",
        "1002000234" to "1002002341",
        "1002002341" to "1002002342",
        "1002002342" to "1002002343",
        "1002002343" to "1002002344",
    )
    val seed_1002 = SubwaySeed.LineSeed(
        // 극성 실측(2026-10-12): updnLine=0 열차가 순번 증가(시청→을지로입구, 내선) 방향으로
        // 이동(본선 14대 중 13대 일치). 예전 값 "1" 은 반대여서 2호선 탑승 매칭이 늘 반대 방향 열차를 잡았다.
        // 성수·신정 지선 셔틀의 극성은 표본이 없어 미검증.
        meta = LineMeta(lineId="1002", lineName="2호선", supportsRealtime=true,
            topology=LineTopology.LOOP, directionPolarity=DirectionPolarity("0"),
            displayName="2호선"),
        stations=stations_1002, edges=edges_1002,
    )

    private val stations_1005: List<Station> = listOf(
        Station(statnId="1005000510", lineId="1005", name="방화", order=0, avgSecToNext=SEC, lat=37.577446, lon=126.812741),
        Station(statnId="1005000511", lineId="1005", name="개화산", order=1, avgSecToNext=SEC, lat=37.572399, lon=126.806171),
        Station(statnId="1005000512", lineId="1005", name="김포공항", order=2, avgSecToNext=SEC, lat=37.562384, lon=126.801292),
        Station(statnId="1005000513", lineId="1005", name="송정", order=3, avgSecToNext=SEC, lat=37.561184, lon=126.811973),
        Station(statnId="1005000514", lineId="1005", name="마곡", order=4, avgSecToNext=SEC, lat=37.560183, lon=126.825448),
        Station(statnId="1005000515", lineId="1005", name="발산", order=5, avgSecToNext=SEC, lat=37.558598, lon=126.837668),
        Station(statnId="1005000516", lineId="1005", name="우장산", order=6, avgSecToNext=SEC, lat=37.548768, lon=126.836318),
        Station(statnId="1005000517", lineId="1005", name="화곡", order=7, avgSecToNext=SEC, lat=37.541513, lon=126.840461),
        Station(statnId="1005000518", lineId="1005", name="까치산", order=8, avgSecToNext=SEC, lat=37.531768, lon=126.846683),
        Station(statnId="1005000519", lineId="1005", name="신정", order=9, avgSecToNext=SEC, lat=37.524997, lon=126.856191),
        Station(statnId="1005000520", lineId="1005", name="목동", order=10, avgSecToNext=SEC, lat=37.526065, lon=126.864931),
        Station(statnId="1005000521", lineId="1005", name="오목교", order=11, avgSecToNext=SEC, lat=37.524496, lon=126.875181),
        Station(statnId="1005000522", lineId="1005", name="양평", order=12, avgSecToNext=SEC, lat=37.525569, lon=126.886129),
        Station(statnId="1005000523", lineId="1005", name="영등포구청", order=13, avgSecToNext=SEC, lat=37.5242, lon=126.89503),
        Station(statnId="1005000524", lineId="1005", name="영등포시장", order=14, avgSecToNext=SEC, lat=37.522669, lon=126.905139),
        Station(statnId="1005000525", lineId="1005", name="신길", order=15, avgSecToNext=SEC, lat=37.517623, lon=126.914839),
        Station(statnId="1005000526", lineId="1005", name="여의도", order=16, avgSecToNext=SEC, lat=37.521747, lon=126.924357),
        Station(statnId="1005000527", lineId="1005", name="여의나루", order=17, avgSecToNext=SEC, lat=37.527098, lon=126.932901),
        Station(statnId="1005000528", lineId="1005", name="마포", order=18, avgSecToNext=SEC, lat=37.539574, lon=126.945932),
        Station(statnId="1005000529", lineId="1005", name="공덕", order=19, avgSecToNext=SEC, lat=37.544431, lon=126.951372),
        Station(statnId="1005000530", lineId="1005", name="애오개", order=20, avgSecToNext=SEC, lat=37.553736, lon=126.95682),
        Station(statnId="1005000531", lineId="1005", name="충정로", order=21, avgSecToNext=SEC, lat=37.560236, lon=126.9629),
        Station(statnId="1005000532", lineId="1005", name="서대문", order=22, avgSecToNext=SEC, lat=37.565773, lon=126.966641),
        Station(statnId="1005000533", lineId="1005", name="광화문", order=23, avgSecToNext=SEC, lat=37.571525, lon=126.97717),
        Station(statnId="1005000534", lineId="1005", name="종로3가", order=24, avgSecToNext=SEC, lat=37.57254, lon=126.990305),
        Station(statnId="1005000535", lineId="1005", name="을지로4가", order=25, avgSecToNext=SEC, lat=37.567352, lon=126.998032),
        Station(statnId="1005000536", lineId="1005", name="동대문역사문화공원", order=26, avgSecToNext=SEC, lat=37.564665, lon=127.005353),
        Station(statnId="1005000537", lineId="1005", name="청구", order=27, avgSecToNext=SEC, lat=37.560276, lon=127.013639),
        Station(statnId="1005000538", lineId="1005", name="신금호", order=28, avgSecToNext=SEC, lat=37.554548, lon=127.020331),
        Station(statnId="1005000539", lineId="1005", name="행당", order=29, avgSecToNext=SEC, lat=37.557322, lon=127.029476),
        Station(statnId="1005000540", lineId="1005", name="왕십리", order=30, avgSecToNext=SEC, lat=37.56184, lon=127.037059),
        Station(statnId="1005000541", lineId="1005", name="마장", order=31, avgSecToNext=SEC, lat=37.5661, lon=127.042973),
        Station(statnId="1005000542", lineId="1005", name="답십리", order=32, avgSecToNext=SEC, lat=37.566747, lon=127.052704),
        Station(statnId="1005000543", lineId="1005", name="장한평", order=33, avgSecToNext=SEC, lat=37.56144, lon=127.064623),
        Station(statnId="1005000544", lineId="1005", name="군자(능동)", order=34, avgSecToNext=SEC, lat=37.557088, lon=127.079577),
        Station(statnId="1005000545", lineId="1005", name="아차산", order=35, avgSecToNext=SEC, lat=37.551691, lon=127.089761),
        Station(statnId="1005000546", lineId="1005", name="광나루", order=36, avgSecToNext=SEC, lat=37.545303, lon=127.10357),
        Station(statnId="1005000547", lineId="1005", name="천호", order=37, avgSecToNext=SEC, lat=37.53864, lon=127.123308),
        Station(statnId="1005000548", lineId="1005", name="강동", order=38, avgSecToNext=SEC, lat=37.535804, lon=127.132481),
        Station(statnId="1005000549", lineId="1005", name="길동", order=39, avgSecToNext=SEC, lat=37.537801, lon=127.140004),
        Station(statnId="1005000550", lineId="1005", name="굽은다리", order=40, avgSecToNext=SEC, lat=37.545477, lon=127.142853),
        Station(statnId="1005000551", lineId="1005", name="명일", order=41, avgSecToNext=SEC, lat=37.55137, lon=127.143999),
        Station(statnId="1005000552", lineId="1005", name="고덕", order=42, avgSecToNext=SEC, lat=37.555004, lon=127.154151),
        Station(statnId="1005000553", lineId="1005", name="상일동", order=43, avgSecToNext=SEC, lat=37.556712, lon=127.166417),
        Station(statnId="1005000554", lineId="1005", name="강일", order=44, avgSecToNext=SEC, lat=37.55749, lon=127.17593),
        Station(statnId="1005000555", lineId="1005", name="미사", order=45, avgSecToNext=SEC, lat=37.560927, lon=127.193877),
        Station(statnId="1005000556", lineId="1005", name="하남풍산", order=46, avgSecToNext=SEC, lat=37.552034, lon=127.203864),
        Station(statnId="1005000557", lineId="1005", name="하남시청", order=47, avgSecToNext=SEC, lat=37.54205, lon=127.20612),
        Station(statnId="1005000558", lineId="1005", name="하남검단산", order=48, avgSecToNext=SEC, lat=37.53972, lon=127.22345),
        Station(statnId="1005080549", lineId="1005", name="둔촌동", order=49, avgSecToNext=SEC, lat=37.527788, lon=127.136248),
        Station(statnId="1005080550", lineId="1005", name="올림픽공원(한국체대)", order=50, avgSecToNext=SEC, lat=37.516201, lon=127.130923),
        Station(statnId="1005080551", lineId="1005", name="방이", order=51, avgSecToNext=SEC, lat=37.508857, lon=127.126133),
        Station(statnId="1005080552", lineId="1005", name="오금", order=52, avgSecToNext=SEC, lat=37.502057, lon=127.127938),
        Station(statnId="1005080553", lineId="1005", name="개롱", order=53, avgSecToNext=SEC, lat=37.498079, lon=127.13482),
        Station(statnId="1005080554", lineId="1005", name="거여", order=54, avgSecToNext=SEC, lat=37.493105, lon=127.14415),
        Station(statnId="1005080555", lineId="1005", name="마천", order=55, avgSecToNext=SEC, lat=37.49499, lon=127.152781),
    )
    private val edges_1005: List<Pair<String, String>> = listOf(
        "1005000510" to "1005000511",
        "1005000511" to "1005000512",
        "1005000512" to "1005000513",
        "1005000513" to "1005000514",
        "1005000514" to "1005000515",
        "1005000515" to "1005000516",
        "1005000516" to "1005000517",
        "1005000517" to "1005000518",
        "1005000518" to "1005000519",
        "1005000519" to "1005000520",
        "1005000520" to "1005000521",
        "1005000521" to "1005000522",
        "1005000522" to "1005000523",
        "1005000523" to "1005000524",
        "1005000524" to "1005000525",
        "1005000525" to "1005000526",
        "1005000526" to "1005000527",
        "1005000527" to "1005000528",
        "1005000528" to "1005000529",
        "1005000529" to "1005000530",
        "1005000530" to "1005000531",
        "1005000531" to "1005000532",
        "1005000532" to "1005000533",
        "1005000533" to "1005000534",
        "1005000534" to "1005000535",
        "1005000535" to "1005000536",
        "1005000536" to "1005000537",
        "1005000537" to "1005000538",
        "1005000538" to "1005000539",
        "1005000539" to "1005000540",
        "1005000540" to "1005000541",
        "1005000541" to "1005000542",
        "1005000542" to "1005000543",
        "1005000543" to "1005000544",
        "1005000544" to "1005000545",
        "1005000545" to "1005000546",
        "1005000546" to "1005000547",
        "1005000547" to "1005000548",
        "1005000548" to "1005000549",
        "1005000549" to "1005000550",
        "1005000550" to "1005000551",
        "1005000551" to "1005000552",
        "1005000552" to "1005000553",
        "1005000553" to "1005000554",
        "1005000554" to "1005000555",
        "1005000555" to "1005000556",
        "1005000556" to "1005000557",
        "1005000557" to "1005000558",
        "1005000548" to "1005080549",
        "1005080549" to "1005080550",
        "1005080550" to "1005080551",
        "1005080551" to "1005080552",
        "1005080552" to "1005080553",
        "1005080553" to "1005080554",
        "1005080554" to "1005080555",
    )
    val seed_1005 = SubwaySeed.LineSeed(
        meta = LineMeta(lineId="1005", lineName="5호선", supportsRealtime=true,
            topology=LineTopology.BRANCHED, directionPolarity=DirectionPolarity("1"),
            displayName="5호선"),
        stations=stations_1005, edges=edges_1005,
    )

    private val stations_1008: List<Station> = listOf(
        Station(statnId="1008000804", lineId="1008", name="별내", order=0, avgSecToNext=SEC, lat=37.64202, lon=127.12684),
        Station(statnId="1008000805", lineId="1008", name="다산", order=1, avgSecToNext=SEC, lat=37.62429, lon=127.14973),
        Station(statnId="1008000806", lineId="1008", name="동구릉", order=2, avgSecToNext=SEC, lat=37.61105, lon=127.13793),
        Station(statnId="1008000807", lineId="1008", name="구리", order=3, avgSecToNext=SEC, lat=37.603392, lon=127.143869),
        Station(statnId="1008000808", lineId="1008", name="장자호수공원", order=4, avgSecToNext=SEC, lat=37.58738, lon=127.13794),
        Station(statnId="1008000809", lineId="1008", name="암사역사공원", order=5, avgSecToNext=SEC, lat=37.55695, lon=127.13702),
        Station(statnId="1008000810", lineId="1008", name="암사", order=6, avgSecToNext=SEC, lat=37.55021, lon=127.127562),
        Station(statnId="1008000811", lineId="1008", name="천호", order=7, avgSecToNext=SEC, lat=37.538113, lon=127.123254),
        Station(statnId="1008000812", lineId="1008", name="강동구청", order=8, avgSecToNext=SEC, lat=37.530341, lon=127.120508),
        Station(statnId="1008000813", lineId="1008", name="몽촌토성", order=9, avgSecToNext=SEC, lat=37.517409, lon=127.112359),
        Station(statnId="1008000814", lineId="1008", name="잠실", order=10, avgSecToNext=SEC, lat=37.514692, lon=127.104338),
        Station(statnId="1008000815", lineId="1008", name="석촌", order=11, avgSecToNext=SEC, lat=37.505557, lon=127.106832),
        Station(statnId="1008000816", lineId="1008", name="송파", order=12, avgSecToNext=SEC, lat=37.499703, lon=127.112183),
        Station(statnId="1008000817", lineId="1008", name="가락시장", order=13, avgSecToNext=SEC, lat=37.492888, lon=127.118398),
        Station(statnId="1008000818", lineId="1008", name="문정", order=14, avgSecToNext=SEC, lat=37.485855, lon=127.1225),
        Station(statnId="1008000819", lineId="1008", name="장지", order=15, avgSecToNext=SEC, lat=37.478703, lon=127.126191),
        Station(statnId="1008000820", lineId="1008", name="복정", order=16, avgSecToNext=SEC, lat=37.471052, lon=127.126732),
        Station(statnId="1008000821", lineId="1008", name="남위례", order=17, avgSecToNext=SEC, lat=37.4624, lon=127.13977),
        Station(statnId="1008000822", lineId="1008", name="산성", order=18, avgSecToNext=SEC, lat=37.457122, lon=127.149908),
        Station(statnId="1008000823", lineId="1008", name="남한산성입구", order=19, avgSecToNext=SEC, lat=37.451535, lon=127.159816),
        Station(statnId="1008000824", lineId="1008", name="단대오거리", order=20, avgSecToNext=SEC, lat=37.44521, lon=127.156866),
        Station(statnId="1008000825", lineId="1008", name="신흥", order=21, avgSecToNext=SEC, lat=37.440918, lon=127.147564),
        Station(statnId="1008000826", lineId="1008", name="수진", order=22, avgSecToNext=SEC, lat=37.437428, lon=127.140722),
        Station(statnId="1008000827", lineId="1008", name="모란", order=23, avgSecToNext=SEC, lat=37.433824, lon=127.129837),
    )
    private val edges_1008: List<Pair<String, String>> = listOf(
        "1008000804" to "1008000805",
        "1008000805" to "1008000806",
        "1008000806" to "1008000807",
        "1008000807" to "1008000808",
        "1008000808" to "1008000809",
        "1008000809" to "1008000810",
        "1008000810" to "1008000811",
        "1008000811" to "1008000812",
        "1008000812" to "1008000813",
        "1008000813" to "1008000814",
        "1008000814" to "1008000815",
        "1008000815" to "1008000816",
        "1008000816" to "1008000817",
        "1008000817" to "1008000818",
        "1008000818" to "1008000819",
        "1008000819" to "1008000820",
        "1008000820" to "1008000821",
        "1008000821" to "1008000822",
        "1008000822" to "1008000823",
        "1008000823" to "1008000824",
        "1008000824" to "1008000825",
        "1008000825" to "1008000826",
        "1008000826" to "1008000827",
    )
    val seed_1008 = SubwaySeed.LineSeed(
        meta = LineMeta(lineId="1008", lineName="8호선", supportsRealtime=true,
            topology=LineTopology.LINEAR, directionPolarity=DirectionPolarity("1"),
            displayName="8호선"),
        stations=stations_1008, edges=edges_1008,
    )

    private val stations_1063: List<Station> = listOf(
        Station(statnId="1063000337", lineId="1063", name="임진강", order=0, avgSecToNext=SEC, lat=37.888421, lon=126.746765),
        Station(statnId="1063000336", lineId="1063", name="운천", order=1, avgSecToNext=SEC, lat=37.879942, lon=126.769999),
        Station(statnId="1063075335", lineId="1063", name="문산", order=2, avgSecToNext=SEC, lat=37.854619, lon=126.788047),
        Station(statnId="1063000334", lineId="1063", name="파주", order=3, avgSecToNext=SEC, lat=37.815298, lon=126.792783),
        Station(statnId="1063000333", lineId="1063", name="월롱", order=4, avgSecToNext=SEC, lat=37.796188, lon=126.792587),
        Station(statnId="1063075331", lineId="1063", name="금촌", order=5, avgSecToNext=SEC, lat=37.766217, lon=126.774644),
        Station(statnId="1063000330", lineId="1063", name="금릉", order=6, avgSecToNext=SEC, lat=37.751322, lon=126.765347),
        Station(statnId="1063075329", lineId="1063", name="운정", order=7, avgSecToNext=SEC, lat=37.725826, lon=126.767257),
        Station(statnId="1063000328", lineId="1063", name="야당", order=8, avgSecToNext=SEC, lat=37.712327, lon=126.761356),
        Station(statnId="1063000327", lineId="1063", name="탄현", order=9, avgSecToNext=SEC, lat=37.694023, lon=126.761086),
        Station(statnId="1063075326", lineId="1063", name="일산", order=10, avgSecToNext=SEC, lat=37.682077, lon=126.769846),
        Station(statnId="1063075325", lineId="1063", name="풍산", order=11, avgSecToNext=SEC, lat=37.672346, lon=126.786243),
        Station(statnId="1063075324", lineId="1063", name="백마", order=12, avgSecToNext=SEC, lat=37.658239, lon=126.794461),
        Station(statnId="1063000323", lineId="1063", name="곡산", order=13, avgSecToNext=SEC, lat=37.645676, lon=126.801762),
        Station(statnId="1063000322", lineId="1063", name="대곡", order=14, avgSecToNext=SEC, lat=37.631626, lon=126.811024),
        Station(statnId="1063000321", lineId="1063", name="능곡", order=15, avgSecToNext=SEC, lat=37.618808, lon=126.820783),
        Station(statnId="1063000320", lineId="1063", name="행신", order=16, avgSecToNext=SEC, lat=37.612102, lon=126.834146),
        Station(statnId="1063000319", lineId="1063", name="강매", order=17, avgSecToNext=SEC, lat=37.612314, lon=126.843223),
        Station(statnId="1063075318", lineId="1063", name="한국항공대", order=18, avgSecToNext=SEC, lat=37.602888, lon=126.868387), // 구 화전
        Station(statnId="1063000317", lineId="1063", name="수색", order=19, avgSecToNext=SEC, lat=37.580842, lon=126.895611),
        Station(statnId="1063075316", lineId="1063", name="디지털미디어시티", order=20, avgSecToNext=SEC, lat=37.577475, lon=126.900453),
        Station(statnId="1063000315", lineId="1063", name="가좌", order=21, avgSecToNext=SEC, lat=37.568491, lon=126.915487),
        Station(statnId="1063075314", lineId="1063", name="홍대입구", order=22, avgSecToNext=SEC, lat=37.557641, lon=126.926683),
        Station(statnId="1063000316", lineId="1063", name="서강대", order=23, avgSecToNext=SEC, lat=37.551881, lon=126.935711),
        Station(statnId="1063000314", lineId="1063", name="공덕", order=24, avgSecToNext=SEC, lat=37.542596, lon=126.952099),
        Station(statnId="1063000826", lineId="1063", name="효창공원앞", order=25, avgSecToNext=SEC, lat=37.538579, lon=126.96221),
        Station(statnId="1063075110", lineId="1063", name="용산", order=26, avgSecToNext=SEC, lat=37.529849, lon=126.964561),
        Station(statnId="1063075111", lineId="1063", name="이촌(국립중앙박물관)", order=27, avgSecToNext=SEC, lat=37.522295, lon=126.974733),
        Station(statnId="1063075112", lineId="1063", name="서빙고", order=28, avgSecToNext=SEC, lat=37.519594, lon=126.988537),
        Station(statnId="1063000113", lineId="1063", name="한남", order=29, avgSecToNext=SEC, lat=37.52943, lon=127.009169),
        Station(statnId="1063000114", lineId="1063", name="옥수", order=30, avgSecToNext=SEC, lat=37.541684, lon=127.017269),
        Station(statnId="1063075115", lineId="1063", name="응봉", order=31, avgSecToNext=SEC, lat=37.549946, lon=127.034538),
        Station(statnId="1063075116", lineId="1063", name="왕십리", order=32, avgSecToNext=SEC, lat=37.561238, lon=127.036954),
        Station(statnId="1063000117", lineId="1063", name="청량리(서울시립대입구)", order=33, avgSecToNext=SEC, lat=37.579956, lon=127.044585),
        Station(statnId="1063075118", lineId="1063", name="회기", order=34, avgSecToNext=SEC, lat=37.58946, lon=127.057583),
        Station(statnId="1063075119", lineId="1063", name="중랑", order=35, avgSecToNext=SEC, lat=37.594917, lon=127.076116),
        Station(statnId="1063000120", lineId="1063", name="상봉(시외버스터미널)", order=36, avgSecToNext=SEC, lat=37.596678, lon=127.08504),
        Station(statnId="1063000121", lineId="1063", name="망우", order=37, avgSecToNext=SEC, lat=37.59955, lon=127.091909),
        Station(statnId="1063000122", lineId="1063", name="양원", order=38, avgSecToNext=SEC, lat=37.606596, lon=127.107906),
        Station(statnId="1063075123", lineId="1063", name="구리", order=39, avgSecToNext=SEC, lat=37.603392, lon=127.143869),
        Station(statnId="1063000124", lineId="1063", name="도농", order=40, avgSecToNext=SEC, lat=37.608806, lon=127.161153),
        Station(statnId="1063000125", lineId="1063", name="양정", order=41, avgSecToNext=SEC, lat=37.60533, lon=127.19364),
        Station(statnId="1063000126", lineId="1063", name="덕소", order=42, avgSecToNext=SEC, lat=37.586781, lon=127.208832),
        Station(statnId="1063075127", lineId="1063", name="도심", order=43, avgSecToNext=SEC, lat=37.579622, lon=127.222672),
        Station(statnId="1063000128", lineId="1063", name="팔당", order=44, avgSecToNext=SEC, lat=37.547371, lon=127.243939),
        Station(statnId="1063000129", lineId="1063", name="운길산", order=45, avgSecToNext=SEC, lat=37.554669, lon=127.310115),
        Station(statnId="1063075130", lineId="1063", name="양수", order=46, avgSecToNext=SEC, lat=37.545981, lon=127.329098),
        Station(statnId="1063000131", lineId="1063", name="신원", order=47, avgSecToNext=SEC, lat=37.525545, lon=127.372921),
        Station(statnId="1063000132", lineId="1063", name="국수", order=48, avgSecToNext=SEC, lat=37.516169, lon=127.399367),
        Station(statnId="1063000133", lineId="1063", name="아신", order=49, avgSecToNext=SEC, lat=37.51382, lon=127.443173),
        Station(statnId="1063000134", lineId="1063", name="오빈", order=50, avgSecToNext=SEC, lat=37.506062, lon=127.473868),
        // 5호선 양평(영등포구)과 이름만 같은 경기 양평군 역(약 53km) — 환승 오연결 방지 구분 표기.
        Station(statnId="1063000135", lineId="1063", name="양평(경의중앙선)", order=51, avgSecToNext=SEC, lat=37.492773, lon=127.491837),
        Station(statnId="1063075136", lineId="1063", name="원덕", order=52, avgSecToNext=SEC, lat=37.468672, lon=127.547076),
        Station(statnId="1063000137", lineId="1063", name="용문", order=53, avgSecToNext=SEC, lat=37.48223, lon=127.594647),
        Station(statnId="1063000138", lineId="1063", name="지평", order=54, avgSecToNext=SEC, lat=37.476393, lon=127.629874),
        // 2호선 신촌과 물리적으로 다른 역(약 700m) — 역명 기반 환승 오연결을 막기 위해 구분 표기.
        Station(statnId="1063000312", lineId="1063", name="신촌(경의중앙선)", order=55, avgSecToNext=SEC, lat=37.559733, lon=126.942597),
        Station(statnId="1063000313", lineId="1063", name="서울역", order=56, avgSecToNext=SEC, lat=37.557231, lon=126.97103),
    )
    private val edges_1063: List<Pair<String, String>> = listOf(
        "1063000337" to "1063000336",  // 임진강 ↔ 운천
        "1063000336" to "1063075335",  // 운천 ↔ 문산
        "1063075335" to "1063000334",  // 문산 ↔ 파주
        "1063000334" to "1063000333",  // 파주 ↔ 월롱
        "1063000333" to "1063075331",  // 월롱 ↔ 금촌
        "1063075331" to "1063000330",  // 금촌 ↔ 금릉
        "1063000330" to "1063075329",  // 금릉 ↔ 운정
        "1063075329" to "1063000328",  // 운정 ↔ 야당
        "1063000328" to "1063000327",  // 야당 ↔ 탄현
        "1063000327" to "1063075326",  // 탄현 ↔ 일산
        "1063075326" to "1063075325",  // 일산 ↔ 풍산
        "1063075325" to "1063075324",  // 풍산 ↔ 백마
        "1063075324" to "1063000323",  // 백마 ↔ 곡산
        "1063000323" to "1063000322",  // 곡산 ↔ 대곡
        "1063000322" to "1063000321",  // 대곡 ↔ 능곡
        "1063000321" to "1063000320",  // 능곡 ↔ 행신
        "1063000320" to "1063000319",  // 행신 ↔ 강매
        "1063000319" to "1063075318",  // 강매 ↔ 한국항공대
        "1063075318" to "1063000317",  // 한국항공대 ↔ 수색
        "1063000317" to "1063075316",  // 수색 ↔ 디지털미디어시티
        "1063075316" to "1063000315",  // 디지털미디어시티 ↔ 가좌
        "1063000315" to "1063075314",  // 가좌 ↔ 홍대입구
        "1063075314" to "1063000316",  // 홍대입구 ↔ 서강대
        "1063000316" to "1063000314",  // 서강대 ↔ 공덕
        "1063000314" to "1063000826",  // 공덕 ↔ 효창공원앞
        "1063000826" to "1063075110",  // 효창공원앞 ↔ 용산
        "1063075110" to "1063075111",  // 용산 ↔ 이촌(국립중앙박물관)
        "1063075111" to "1063075112",  // 이촌(국립중앙박물관) ↔ 서빙고
        "1063075112" to "1063000113",  // 서빙고 ↔ 한남
        "1063000113" to "1063000114",  // 한남 ↔ 옥수
        "1063000114" to "1063075115",  // 옥수 ↔ 응봉
        "1063075115" to "1063075116",  // 응봉 ↔ 왕십리
        "1063075116" to "1063000117",  // 왕십리 ↔ 청량리(서울시립대입구)
        "1063000117" to "1063075118",  // 청량리(서울시립대입구) ↔ 회기
        "1063075118" to "1063075119",  // 회기 ↔ 중랑
        "1063075119" to "1063000120",  // 중랑 ↔ 상봉(시외버스터미널)
        "1063000120" to "1063000121",  // 상봉(시외버스터미널) ↔ 망우
        "1063000121" to "1063000122",  // 망우 ↔ 양원
        "1063000122" to "1063075123",  // 양원 ↔ 구리
        "1063075123" to "1063000124",  // 구리 ↔ 도농
        "1063000124" to "1063000125",  // 도농 ↔ 양정
        "1063000125" to "1063000126",  // 양정 ↔ 덕소
        "1063000126" to "1063075127",  // 덕소 ↔ 도심
        "1063075127" to "1063000128",  // 도심 ↔ 팔당
        "1063000128" to "1063000129",  // 팔당 ↔ 운길산
        "1063000129" to "1063075130",  // 운길산 ↔ 양수
        "1063075130" to "1063000131",  // 양수 ↔ 신원
        "1063000131" to "1063000132",  // 신원 ↔ 국수
        "1063000132" to "1063000133",  // 국수 ↔ 아신
        "1063000133" to "1063000134",  // 아신 ↔ 오빈
        "1063000134" to "1063000135",  // 오빈 ↔ 양평(경의중앙선)
        "1063000135" to "1063075136",  // 양평(경의중앙선) ↔ 원덕
        "1063075136" to "1063000137",  // 원덕 ↔ 용문
        "1063000137" to "1063000138",  // 용문 ↔ 지평
        "1063000315" to "1063000312",  // 가좌 ↔ 신촌(경의중앙선)
        "1063000312" to "1063000313",  // 신촌(경의중앙선) ↔ 서울역
    )
    val seed_1063 = SubwaySeed.LineSeed(
        meta = LineMeta(lineId="1063", lineName="경의중앙선", supportsRealtime=true,
            topology=LineTopology.BRANCHED, directionPolarity=DirectionPolarity("1"),
            displayName="경의중앙선"),
        stations=stations_1063, edges=edges_1063,
    )

    private val stations_1075: List<Station> = listOf(
        Station(statnId="1075000209", lineId="1075", name="청량리(서울시립대입구)", order=0, avgSecToNext=SEC, lat=37.579956, lon=127.044585),
        Station(statnId="1075075210", lineId="1075", name="왕십리", order=1, avgSecToNext=SEC, lat=37.561238, lon=127.036954),
        Station(statnId="1075000211", lineId="1075", name="서울숲", order=2, avgSecToNext=SEC, lat=37.543617, lon=127.044707),
        Station(statnId="1075075212", lineId="1075", name="압구정로데오", order=3, avgSecToNext=SEC, lat=37.527381, lon=127.040534),
        Station(statnId="1075075213", lineId="1075", name="강남구청", order=4, avgSecToNext=SEC, lat=37.517469, lon=127.041151),
        Station(statnId="1075000214", lineId="1075", name="선정릉", order=5, avgSecToNext=SEC, lat=37.510735, lon=127.043677),
        Station(statnId="1075075215", lineId="1075", name="선릉", order=6, avgSecToNext=SEC, lat=37.504856, lon=127.048807),
        Station(statnId="1075075216", lineId="1075", name="한티", order=7, avgSecToNext=SEC, lat=37.496237, lon=127.052873),
        Station(statnId="1075000217", lineId="1075", name="도곡", order=8, avgSecToNext=SEC, lat=37.491224, lon=127.055186),
        Station(statnId="1075000218", lineId="1075", name="구룡", order=9, avgSecToNext=SEC, lat=37.486839, lon=127.058856),
        Station(statnId="1075000219", lineId="1075", name="개포동", order=10, avgSecToNext=SEC, lat=37.489116, lon=127.06614),
        Station(statnId="1075000220", lineId="1075", name="대모산입구", order=11, avgSecToNext=SEC, lat=37.491373, lon=127.07272),
        Station(statnId="1075075221", lineId="1075", name="수서", order=12, avgSecToNext=SEC, lat=37.487472, lon=127.101422),
        Station(statnId="1075075222", lineId="1075", name="복정", order=13, avgSecToNext=SEC, lat=37.470047, lon=127.12666),
        Station(statnId="1075000223", lineId="1075", name="가천대", order=14, avgSecToNext=SEC, lat=37.448605, lon=127.126697),
        Station(statnId="1075075224", lineId="1075", name="태평", order=15, avgSecToNext=SEC, lat=37.440019, lon=127.127709),
        Station(statnId="1075075225", lineId="1075", name="모란", order=16, avgSecToNext=SEC, lat=37.432052, lon=127.129104),
        Station(statnId="1075075226", lineId="1075", name="야탑", order=17, avgSecToNext=SEC, lat=37.411185, lon=127.128715),
        Station(statnId="1075000227", lineId="1075", name="이매", order=18, avgSecToNext=SEC, lat=37.395371, lon=127.128248),
        Station(statnId="1075075228", lineId="1075", name="서현", order=19, avgSecToNext=SEC, lat=37.385126, lon=127.123592),
        Station(statnId="1075000229", lineId="1075", name="수내", order=20, avgSecToNext=SEC, lat=37.378455, lon=127.114322),
        Station(statnId="1075075230", lineId="1075", name="정자", order=21, avgSecToNext=SEC, lat=37.365994, lon=127.10807),
        Station(statnId="1075075231", lineId="1075", name="미금", order=22, avgSecToNext=SEC, lat=37.350077, lon=127.10891),
        Station(statnId="1075000232", lineId="1075", name="오리", order=23, avgSecToNext=SEC, lat=37.339824, lon=127.108942),
        Station(statnId="1075075233", lineId="1075", name="죽전", order=24, avgSecToNext=SEC, lat=37.324753, lon=127.107395),
        Station(statnId="1075075234", lineId="1075", name="보정", order=25, avgSecToNext=SEC, lat=37.312752, lon=127.108196),
        Station(statnId="1075000235", lineId="1075", name="구성", order=26, avgSecToNext=SEC, lat=37.298969, lon=127.105664),
        Station(statnId="1075000236", lineId="1075", name="신갈", order=27, avgSecToNext=SEC, lat=37.286102, lon=127.111313),
        Station(statnId="1075000237", lineId="1075", name="기흥", order=28, avgSecToNext=SEC, lat=37.275061, lon=127.11591),
        Station(statnId="1075075238", lineId="1075", name="상갈", order=29, avgSecToNext=SEC, lat=37.26181, lon=127.108847),
        Station(statnId="1075000239", lineId="1075", name="청명", order=30, avgSecToNext=SEC, lat=37.259489, lon=127.078934),
        Station(statnId="1075000240", lineId="1075", name="영통", order=31, avgSecToNext=SEC, lat=37.251568, lon=127.071394),
        Station(statnId="1075000241", lineId="1075", name="망포", order=32, avgSecToNext=SEC, lat=37.245795, lon=127.057353),
        Station(statnId="1075075242", lineId="1075", name="매탄권선", order=33, avgSecToNext=SEC, lat=37.252759, lon=127.040566),
        Station(statnId="1075000243", lineId="1075", name="수원시청", order=34, avgSecToNext=SEC, lat=37.261911, lon=127.030736),
        Station(statnId="1075000244", lineId="1075", name="매교", order=35, avgSecToNext=SEC, lat=37.265481, lon=127.015678),
        Station(statnId="1075000245", lineId="1075", name="수원", order=36, avgSecToNext=SEC, lat=37.265917, lon=126.999422),
        Station(statnId="1075075246", lineId="1075", name="고색", order=37, avgSecToNext=SEC, lat=37.24963, lon=126.980248),
        Station(statnId="1075075247", lineId="1075", name="오목천", order=38, avgSecToNext=SEC, lat=37.24304, lon=126.963676),
        Station(statnId="1075075248", lineId="1075", name="어천", order=39, avgSecToNext=SEC, lat=37.250102, lon=126.90879),
        Station(statnId="1075000249", lineId="1075", name="야목", order=40, avgSecToNext=SEC, lat=37.264179, lon=126.879483),
        Station(statnId="1075000250", lineId="1075", name="사리", order=41, avgSecToNext=SEC, lat=37.28998, lon=126.85685),
        Station(statnId="1075075251", lineId="1075", name="한대앞", order=42, avgSecToNext=SEC, lat=37.309689, lon=126.85344),
        Station(statnId="1075075252", lineId="1075", name="중앙", order=43, avgSecToNext=SEC, lat=37.315941, lon=126.838573),
        Station(statnId="1075000253", lineId="1075", name="고잔", order=44, avgSecToNext=SEC, lat=37.316784, lon=126.823144),
        Station(statnId="1075075254", lineId="1075", name="초지", order=45, avgSecToNext=SEC, lat=37.320646, lon=126.805913),
        Station(statnId="1075000255", lineId="1075", name="안산", order=46, avgSecToNext=SEC, lat=37.327082, lon=126.788532),
        Station(statnId="1075000256", lineId="1075", name="신길온천", order=47, avgSecToNext=SEC, lat=37.338212, lon=126.765844),
        Station(statnId="1075000257", lineId="1075", name="정왕", order=48, avgSecToNext=SEC, lat=37.351735, lon=126.742989),
        Station(statnId="1075000258", lineId="1075", name="오이도", order=49, avgSecToNext=SEC, lat=37.362357, lon=126.738714),
        Station(statnId="1075000259", lineId="1075", name="달월", order=50, avgSecToNext=SEC, lat=37.379681, lon=126.745177),
        Station(statnId="1075000260", lineId="1075", name="월곶", order=51, avgSecToNext=SEC, lat=37.391769, lon=126.742699),
        Station(statnId="1075075261", lineId="1075", name="소래포구", order=52, avgSecToNext=SEC, lat=37.40095, lon=126.733522),
        Station(statnId="1075075262", lineId="1075", name="인천논현", order=53, avgSecToNext=SEC, lat=37.400614, lon=126.722478),
        Station(statnId="1075000263", lineId="1075", name="호구포", order=54, avgSecToNext=SEC, lat=37.401637, lon=126.708627),
        Station(statnId="1075000264", lineId="1075", name="남동인더스파크", order=55, avgSecToNext=SEC, lat=37.407722, lon=126.695216),
        Station(statnId="1075075265", lineId="1075", name="원인재", order=56, avgSecToNext=SEC, lat=37.413049, lon=126.686648),
        Station(statnId="1075000266", lineId="1075", name="연수", order=57, avgSecToNext=SEC, lat=37.417804, lon=126.67894),
        Station(statnId="1075000267", lineId="1075", name="송도", order=58, avgSecToNext=SEC, lat=37.428514, lon=126.657772),
        Station(statnId="1075000269", lineId="1075", name="인하대", order=59, avgSecToNext=SEC, lat=37.448493, lon=126.649619),
        Station(statnId="1075075269", lineId="1075", name="숭의", order=60, avgSecToNext=SEC, lat=37.460789, lon=126.638297),
        Station(statnId="1075000271", lineId="1075", name="신포", order=61, avgSecToNext=SEC, lat=37.46874, lon=126.623853),
        Station(statnId="1075000272", lineId="1075", name="인천", order=62, avgSecToNext=SEC, lat=37.476403, lon=126.617326),
    )
    val seed_1075 = SubwaySeed.LineSeed(
        meta = LineMeta(lineId="1075", lineName="수인분당선", supportsRealtime=true,
            topology=LineTopology.LINEAR, directionPolarity=DirectionPolarity("1"),
            displayName="수인분당선"),
        stations=stations_1075,
    )

    private val stations_1077: List<Station> = listOf(
        Station(statnId="1077000684", lineId="1077", name="신사", order=0, avgSecToNext=SEC, lat=37.516334, lon=127.020114),
        Station(statnId="1077000005", lineId="1077", name="논현", order=1, avgSecToNext=SEC, lat=37.511093, lon=127.021415),
        Station(statnId="1077000006", lineId="1077", name="신논현", order=2, avgSecToNext=SEC, lat=37.504598, lon=127.02506),
        Station(statnId="1077000687", lineId="1077", name="강남", order=3, avgSecToNext=SEC, lat=37.496837, lon=127.028104),
        Station(statnId="1077000688", lineId="1077", name="양재(서초구청)", order=4, avgSecToNext=SEC, lat=37.483809, lon=127.034653),
        Station(statnId="1077000009", lineId="1077", name="양재시민의숲", order=5, avgSecToNext=SEC, lat=37.470023, lon=127.03842),
        Station(statnId="1077006810", lineId="1077", name="청계산입구", order=6, avgSecToNext=SEC, lat=37.447211, lon=127.055664),
        Station(statnId="1077000011", lineId="1077", name="판교", order=7, avgSecToNext=SEC, lat=37.394761, lon=127.112217),
        Station(statnId="1077006812", lineId="1077", name="정자", order=8, avgSecToNext=SEC, lat=37.367098, lon=127.108403),
        Station(statnId="1077000013", lineId="1077", name="미금", order=9, avgSecToNext=SEC, lat=37.349982, lon=127.108918),
        Station(statnId="1077006814", lineId="1077", name="동천", order=10, avgSecToNext=SEC, lat=37.337928, lon=127.102976),
        Station(statnId="1077006815", lineId="1077", name="수지구청", order=11, avgSecToNext=SEC, lat=37.322702, lon=127.095026),
        Station(statnId="1077000016", lineId="1077", name="성복", order=12, avgSecToNext=SEC, lat=37.313335, lon=127.0801),
        Station(statnId="1077006817", lineId="1077", name="상현", order=13, avgSecToNext=SEC, lat=37.297664, lon=127.069342),
        Station(statnId="1077006818", lineId="1077", name="광교중앙", order=14, avgSecToNext=SEC, lat=37.288617, lon=127.051478),
        Station(statnId="1077006819", lineId="1077", name="광교", order=15, avgSecToNext=SEC, lat=37.30211, lon=127.044483),
    )
    val seed_1077 = SubwaySeed.LineSeed(
        meta = LineMeta(lineId="1077", lineName="신분당선", supportsRealtime=true,
            topology=LineTopology.LINEAR, directionPolarity=DirectionPolarity("1"),
            displayName="신분당선"),
        stations=stations_1077,
    )

    private val stations_1065: List<Station> = listOf(
        Station(statnId="1065000001", lineId="1065", name="서울역", order=0, avgSecToNext=SEC, lat=37.553247, lon=126.969769),
        Station(statnId="1065006502", lineId="1065", name="공덕", order=1, avgSecToNext=SEC, lat=37.54253, lon=126.952024),
        Station(statnId="1065006503", lineId="1065", name="홍대입구", order=2, avgSecToNext=SEC, lat=37.557438, lon=126.926715),
        Station(statnId="1065006504", lineId="1065", name="디지털미디어시티", order=3, avgSecToNext=SEC, lat=37.576958, lon=126.898609),
        Station(statnId="1065065042", lineId="1065", name="마곡나루", order=4, avgSecToNext=SEC, lat=37.565543, lon=126.827378),
        Station(statnId="1065006505", lineId="1065", name="김포공항", order=5, avgSecToNext=SEC, lat=37.561842, lon=126.801904),
        Station(statnId="1065006506", lineId="1065", name="계양", order=6, avgSecToNext=SEC, lat=37.571662, lon=126.7363),
        Station(statnId="1065006507", lineId="1065", name="검암", order=7, avgSecToNext=SEC, lat=37.569098, lon=126.674007),
        Station(statnId="1065065071", lineId="1065", name="청라국제도시", order=8, avgSecToNext=SEC, lat=37.556409, lon=126.624648),
        Station(statnId="1065065072", lineId="1065", name="영종", order=9, avgSecToNext=SEC, lat=37.51202, lon=126.524254),
        Station(statnId="1065000008", lineId="1065", name="운서", order=10, avgSecToNext=SEC, lat=37.492904, lon=126.49379),
        Station(statnId="1065006509", lineId="1065", name="공항화물청사", order=11, avgSecToNext=SEC, lat=37.459041, lon=126.477516),
        Station(statnId="1065006510", lineId="1065", name="인천공항1터미널", order=12, avgSecToNext=SEC, lat=37.447464, lon=126.452508),
        Station(statnId="1065000011", lineId="1065", name="인천공항2터미널", order=13, avgSecToNext=SEC, lat=37.460699, lon=126.441442),
    )
    val seed_1065 = SubwaySeed.LineSeed(
        meta = LineMeta(lineId="1065", lineName="공항철도", supportsRealtime=true,
            topology=LineTopology.LINEAR, directionPolarity=DirectionPolarity("1"),
            displayName="공항철도"),
        stations=stations_1065,
    )

    private val stations_1067: List<Station> = listOf(
        Station(statnId="1067000116", lineId="1067", name="광운대", order=0, avgSecToNext=SEC, lat=37.623632, lon=127.061835),
        Station(statnId="1067000117", lineId="1067", name="청량리(서울시립대입구)", order=1, avgSecToNext=SEC, lat=37.579956, lon=127.044585),
        Station(statnId="1067000118", lineId="1067", name="회기", order=2, avgSecToNext=SEC, lat=37.58946, lon=127.057583),
        Station(statnId="1067000119", lineId="1067", name="중랑", order=3, avgSecToNext=SEC, lat=37.594917, lon=127.076116),
        Station(statnId="1067080120", lineId="1067", name="상봉(시외버스터미널)", order=4, avgSecToNext=SEC, lat=37.596678, lon=127.08504),
        Station(statnId="1067080121", lineId="1067", name="망우", order=5, avgSecToNext=SEC, lat=37.59955, lon=127.091909),
        Station(statnId="1067000122", lineId="1067", name="신내", order=6, avgSecToNext=SEC, lat=37.612887, lon=127.103218),
        Station(statnId="1067000123", lineId="1067", name="갈매", order=7, avgSecToNext=SEC, lat=37.634118, lon=127.114757),
        Station(statnId="1067000124", lineId="1067", name="별내", order=8, avgSecToNext=SEC, lat=37.64202, lon=127.12684),
        Station(statnId="1067080125", lineId="1067", name="퇴계원", order=9, avgSecToNext=SEC, lat=37.648311, lon=127.143952),
        Station(statnId="1067080126", lineId="1067", name="사릉", order=10, avgSecToNext=SEC, lat=37.65108, lon=127.176933),
        Station(statnId="1067000127", lineId="1067", name="금곡", order=11, avgSecToNext=SEC, lat=37.637382, lon=127.207853),
        Station(statnId="1067000128", lineId="1067", name="평내호평", order=12, avgSecToNext=SEC, lat=37.653225, lon=127.244493),
        Station(statnId="1067000129", lineId="1067", name="천마산", order=13, avgSecToNext=SEC, lat=37.658978, lon=127.285379),
        Station(statnId="1067080130", lineId="1067", name="마석", order=14, avgSecToNext=SEC, lat=37.652782, lon=127.311767),
        Station(statnId="1067000131", lineId="1067", name="대성리", order=15, avgSecToNext=SEC, lat=37.684071, lon=127.379319),
        Station(statnId="1067080132", lineId="1067", name="청평", order=16, avgSecToNext=SEC, lat=37.735488, lon=127.42661),
        Station(statnId="1067000133", lineId="1067", name="상천", order=17, avgSecToNext=SEC, lat=37.770246, lon=127.454821),
        Station(statnId="1067080134", lineId="1067", name="가평", order=18, avgSecToNext=SEC, lat=37.814536, lon=127.510739),
        Station(statnId="1067000135", lineId="1067", name="굴봉산", order=19, avgSecToNext=SEC, lat=37.832067, lon=127.557695),
        Station(statnId="1067000136", lineId="1067", name="백양리", order=20, avgSecToNext=SEC, lat=37.830779, lon=127.58933),
        Station(statnId="1067000137", lineId="1067", name="강촌", order=21, avgSecToNext=SEC, lat=37.805723, lon=127.634146),
        Station(statnId="1067080138", lineId="1067", name="김유정", order=22, avgSecToNext=SEC, lat=37.818466, lon=127.71434),
        Station(statnId="1067000139", lineId="1067", name="남춘천", order=23, avgSecToNext=SEC, lat=37.864007, lon=127.723792),
        Station(statnId="1067080140", lineId="1067", name="춘천", order=24, avgSecToNext=SEC, lat=37.885054, lon=127.717023),
    )
    private val edges_1067: List<Pair<String, String>> = listOf(
        "1067000116" to "1067000117",
        "1067000117" to "1067000118",
        "1067000118" to "1067000119",
        "1067000119" to "1067080120",
        "1067080120" to "1067080121",
        "1067080121" to "1067000122",
        "1067000122" to "1067000123",
        "1067000123" to "1067000124",
        "1067000124" to "1067080125",
        "1067080125" to "1067080126",
        "1067080126" to "1067000127",
        "1067000127" to "1067000128",
        "1067000128" to "1067000129",
        "1067000129" to "1067080130",
        "1067080130" to "1067000131",
        "1067000131" to "1067080132",
        "1067080132" to "1067000133",
        "1067000133" to "1067080134",
        "1067080134" to "1067000135",
        "1067000135" to "1067000136",
        "1067000136" to "1067000137",
        "1067000137" to "1067080138",
        "1067080138" to "1067000139",
        "1067000139" to "1067080140",
    )
    val seed_1067 = SubwaySeed.LineSeed(
        meta = LineMeta(lineId="1067", lineName="경춘선", supportsRealtime=true,
            topology=LineTopology.LINEAR, directionPolarity=DirectionPolarity("1"),
            displayName="경춘선"),
        stations=stations_1067, edges=edges_1067,
    )

    private val stations_1092: List<Station> = listOf(
        Station(statnId="1092004701", lineId="1092", name="북한산우이", order=0, avgSecToNext=SEC, lat=37.662909, lon=127.012706),
        Station(statnId="1092004702", lineId="1092", name="솔밭공원", order=1, avgSecToNext=SEC, lat=37.65603, lon=127.013273),
        Station(statnId="1092000943", lineId="1092", name="4.19민주묘지", order=2, avgSecToNext=SEC, lat=37.649502, lon=127.013684),
        Station(statnId="1092004704", lineId="1092", name="가오리", order=3, avgSecToNext=SEC, lat=37.641537, lon=127.016789),
        Station(statnId="1092004705", lineId="1092", name="화계", order=4, avgSecToNext=SEC, lat=37.634133, lon=127.017511),
        Station(statnId="1092004706", lineId="1092", name="삼양", order=5, avgSecToNext=SEC, lat=37.626914, lon=127.018106),
        Station(statnId="1092000947", lineId="1092", name="삼양사거리", order=6, avgSecToNext=SEC, lat=37.621337, lon=127.020473),
        Station(statnId="1092004708", lineId="1092", name="솔샘", order=7, avgSecToNext=SEC, lat=37.620238, lon=127.013626),
        Station(statnId="1092000949", lineId="1092", name="북한산보국문", order=8, avgSecToNext=SEC, lat=37.612072, lon=127.008251),
        Station(statnId="1092004710", lineId="1092", name="정릉", order=9, avgSecToNext=SEC, lat=37.603133, lon=127.013396),
        Station(statnId="1092000951", lineId="1092", name="성신여대입구(돈암)", order=10, avgSecToNext=SEC, lat=37.592467, lon=127.016516),
        Station(statnId="1092004712", lineId="1092", name="보문", order=11, avgSecToNext=SEC, lat=37.585286, lon=127.019381),
        Station(statnId="1092004713", lineId="1092", name="신설동", order=12, avgSecToNext=SEC, lat=37.576095, lon=127.023242),
    )
    private val edges_1092: List<Pair<String, String>> = listOf(
        "1092004701" to "1092004702",
        "1092004702" to "1092000943",
        "1092000943" to "1092004704",
        "1092004704" to "1092004705",
        "1092004705" to "1092004706",
        "1092004706" to "1092000947",
        "1092000947" to "1092004708",
        "1092004708" to "1092000949",
        "1092000949" to "1092004710",
        "1092004710" to "1092000951",
        "1092000951" to "1092004712",
        "1092004712" to "1092004713",
    )
    val seed_1092 = SubwaySeed.LineSeed(
        meta = LineMeta(lineId="1092", lineName="우이신설선", supportsRealtime=true,
            topology=LineTopology.LINEAR, directionPolarity=DirectionPolarity("1"),
            displayName="우이신설선"),
        stations=stations_1092, edges=edges_1092,
    )

    private val stations_1094: List<Station> = listOf(
        Station(statnId="1094000402", lineId="1094", name="대방", order=0, avgSecToNext=SEC, lat=37.5133059, lon=126.9257265),
        Station(statnId="1094000408", lineId="1094", name="신림", order=1, avgSecToNext=SEC, lat=37.4849266, lon=126.9296159),
        Station(statnId="1094000411", lineId="1094", name="관악산", order=2, avgSecToNext=SEC, lat=37.4691018, lon=126.9450639),
        Station(statnId="1094000406", lineId="1094", name="보라매병원", order=3, avgSecToNext=SEC, lat=37.4929598, lon=126.9234964),
        Station(statnId="1094000407", lineId="1094", name="당곡", order=4, avgSecToNext=SEC, lat=37.4902998, lon=126.9275133),
        Station(statnId="1094000409", lineId="1094", name="서원", order=5, avgSecToNext=SEC, lat=37.4782341, lon=126.9330365),
        Station(statnId="1094000410", lineId="1094", name="서울대벤처타운", order=6, avgSecToNext=SEC, lat=37.4720019, lon=126.9339351),
        Station(statnId="1094000401", lineId="1094", name="샛강", order=7, avgSecToNext=SEC, lat=37.5170969, lon=126.929399),
        Station(statnId="1094000404", lineId="1094", name="보라매", order=8, avgSecToNext=SEC, lat=37.5002739, lon=126.9204355),
        Station(statnId="1094000405", lineId="1094", name="보라매공원", order=9, avgSecToNext=SEC, lat=37.4955691, lon=126.9180827),
        Station(statnId="1094000403", lineId="1094", name="서울지방병무청", order=10, avgSecToNext=SEC, lat=37.5060464, lon=126.9227083),
    )
    private val edges_1094: List<Pair<String, String>> = listOf(
        "1094000402" to "1094000408",
        "1094000408" to "1094000411",
        "1094000411" to "1094000406",
        "1094000406" to "1094000407",
        "1094000407" to "1094000409",
        "1094000409" to "1094000410",
        "1094000410" to "1094000401",
        "1094000401" to "1094000404",
        "1094000404" to "1094000405",
        "1094000405" to "1094000403",
    )
    val seed_1094 = SubwaySeed.LineSeed(
        meta = LineMeta(lineId="1094", lineName="신림선", supportsRealtime=true,
            topology=LineTopology.LINEAR, directionPolarity=DirectionPolarity("1"),
            displayName="신림선"),
        stations=stations_1094, edges=edges_1094,
    )

    val all: List<SubwaySeed.LineSeed> = listOf(
        seed_1002,
        seed_1005,
        seed_1008,
        seed_1063,
        seed_1075,
        seed_1077,
        seed_1065,
        seed_1067,
        seed_1092,
        seed_1094,
    )
}
