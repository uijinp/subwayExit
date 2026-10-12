# HANDOFF — subwayAlarm 전체 구현 (갭 + 데이터 감사)
> 최종 갱신: 2026-10-12 / by Claude (전체 재검토 — §C 발견 사항, 다음 작업 = §C P0 수정)

## 목표
데이터 정합성(D-1~D-4) + Replay + 재매칭 + 가속도계 하이브리드를 구현하고 유닛테스트로 고정.

## 상태
- [x] D-1 이수↔총신대 보행 환승 (`WalkTransfers` + `SubwayNetwork`)
- [x] D-2 1호선 표시명 “경인 청량리–인천” (`LineMeta.displayName`)
- [x] D-3 `statnTnm` → `TrainPosition.terminusName`
- [x] D-4 경부 종착 매칭 제외 (`excludedTerminusNames`)
- [x] Replay R0–R4 (`replay/ReplayTripRunner` + 3 시나리오 테스트)
- [x] 재매칭 M0–M6 (후보 목록, Confirm, TrackingScreen UI, Service 인텐트)
- [x] 가속도계 A0–A7 (`AccelStopCounter`, `HybridCoordinator`, 센서 Flow, TrackingService 연동)
- [x] 유닛테스트 green (`./gradlew :app:testDebugUnitTest` — 145 tests)
- [x] 검토(Claude, 2026-07-16): 전체 테스트·빌드 green 확인. 시드 감사에서 동명이역
      가짜 환승 적발·수정 — 신촌·양평(경의중앙선) 구분 표기, 능곡(서해선)·부평(인천1)·
      주안(인천2) 좌표 오입력 정정, SubwayNetwork 에 600m 좌표 가드,
      `SeedStationNameCollisionTest` 로 재발 방지(노선 추가 시 자동 검증).
- [x] 환승 기능 자체는 동작 확인(Claude, 실측): 내방→사당이 `7호선[내방→이수] ⇄ 4호선[총신대입구(이수)→사당]`
      2구간으로 정상 계산됨(보행 환승 포함). 가락시장→동묘앞(3→6), 부평→강남(1→2 신도림) 정상.
- [x] **§A 광역선 4개 `order`/`edges` 재작성 완료** (Claude, 2026-07-16)
      — 3개(수인분당·공항철도·신분당)는 지리 순서 재부여 후 `edges` 삭제(LINEAR),
      경의중앙선은 재부여 + 가좌–신촌–서울역 지선 엣지 명시 + `topology=BRANCHED`.
      `directionPolarity` 는 4개 노선 모두 실측 재검증(전부 `"1"` 이 맞음, 근거는 시드 헤더 주석).
      `SeedAdjacencyDistanceTest` 추가 — 인접 8km 규칙 + 노선 연결성. 전체 테스트 green.
      Acceptance 실측: 김포공항→잠실 `공항철도[→홍대입구] ⇄ 2호선[→잠실]`,
      수원→판교 `수인분당선[→정자] ⇄ 신분당선[→판교]`, 서울역→일산 경의중앙선 직행(지선 경유 확인).
- [x] **statnId 정규화 (§B) 완료** (Claude, 2026-07-16) — 광역선 시드 statnId 가 API 실제값과
      달라(예: 경의중앙선 옥수 시드 `1063000114` ↔ API `1063075114`) 해당 역에 열차가 있는 동안
      위치를 못 잡고 추적이 멈추던 버그. `NormalizePositionsUseCase`(순수)로 경계에서 한 번
      정규화 — statnId → 역명 → 느슨한 역명(괄호·공백·끝 “역” 무시) 3단계 폴백.
      실측 개선: 경의중앙선 12/19 유실 → **0**, 수인분당선 15/30 → 0, 신분당선 5/12 → 0,
      공항철도 2/18 → 0, 우이신설선 4/14 → 0. (경춘선 ‘용산’ 1건만 남음 = 시드 구간 밖, §B-잔여)
- [x] **즐겨찾기(북마크) 경로 완료** (Claude, 2026-07-16) — 자주 쓰는 출발·도착을 저장하고
      칩을 눌러 바로 불러온다. `FavoriteRouteEntity/Dao/Repository` + DB v4→5
      **실제 마이그레이션**(`SubwayDatabase.MIGRATION_4_5`), 홈에 “⭐ 자주 가는 경로” 칩과
      추적 버튼 옆 ★ 토글. 역명으로 저장하고 경로는 불러올 때 재계산 —
      시드가 바뀌어도 즐겨찾기가 깨지지 않는다. 기기에서 저장→앱 재시작→불러오기 확인.
- [x] **전체 재검토 (Claude, 2026-10-12)** — 빌드 green, 테스트 338개 통과. 코드 대조 + API 실측으로
      알람 누락·오알람·크래시 결함 다수 확인 → **§C** 에 우선순위별 정리.
- [ ] **← 다음 작업**: §C P0-1·P0-2 (2호선·우이신설선 극성 정정 + 시드 버전 기반 재적재) —
      둘은 한 묶음이다. 극성만 고치면 기존 설치에는 반영되지 않는다(§C P0-2 참고).
- [ ] 이후: §C 나머지 P0 → P1 순서. 그다음 실기기 E2E(이수 환승 + 주머니 알람 + “다른 열차?”).
- [ ] 이후(§B-잔여): 시드 구간 밖 열차 — 3호선 일산선(주엽·화정·삼송…), 4호선 과천·안산선(12역),
      경춘선 용산. 해당 구간을 시드에 넣으면 해소되나 지금은 그 구간 여정을 못 만들 뿐 기존 추적엔 무해.
- [ ] 이후: Replay JSON 픽스처를 `test/resources` 로 외부화(현재 코드 픽스처)
- [ ] 이후: 가속도 임계값 실기기 튜닝 (A9)

---

## §C 전체 재검토 결과 (2026-10-12) — 미수정, 다음 작업 목록

검토 방법: 추적 엔진·플랫폼 계층을 나눠 코드 리뷰 → **모든 항목을 코드로 직접 대조**,
극성은 실시간 API 를 55초 간격 2회 조회해 열차 실제 이동 방향으로 실측.
경로는 `app/src/main/java/com/jin/subwayalarm/` 기준.

### P0 — 알람이 안 울리거나, 엉뚱한 데서 울리거나, 앱이 죽는다

| # | 결함 | 위치 | 확인 근거 |
|---|---|---|---|
| P0-1 | **2호선 극성 반대** — `ascendingUpdnLine="1"` 인데 실측은 `"0"`. 2호선 탑승 매칭이 전부 반대 방향 열차를 잡는다. **우이신설선도 반대 유력**(3/3). 신림선은 판정 불가(2/4), 경춘·서해·경강·GTX-A 는 표본 부족 | `data/local/ExtraLinesSeed.kt` 2호선·우이신설선 meta | 실측 2호선 13/14대 일치. 나머지 노선은 ✅ |
| P0-2 | **시드 갱신이 역 “개수” 변화에만 반응** — 순서·이름·극성만 바뀐 수정은 기존 설치의 Room 에 영영 안 들어간다. 극성은 Room(`LineMetaEntity.ascendingUpdnLine`)에서 읽으므로 P0-1 을 고쳐도 기존 사용자에겐 무효. 7월 광역선 순서 수정도 기기에 반영 안 됐을 수 있다 | `data/repository/StationRepositoryImpl.kt:21-31`, `data/local/Mappers.kt:46` | 코드 확인. 해결안: 시드 버전 상수 + 버전 바뀌면 전 노선 트랜잭션 재적재(역은 정적 데이터라 매번 재적재해도 수 ms) |
| P0-3 | **웨이크락 없음** — FGS 는 CPU 를 깨워두지 않는다. 화면 꺼지면 `delay()`(uptime 기반)가 멈춰 폴링이 늦어진다. 사용자 보고 “중간에 추적이 안 됨”의 가장 유력한 원인 | `tracking/TrackingService.kt` 폴링 루프, Manifest(WAKE_LOCK 없음) | grep 결과 PowerManager/WAKE_LOCK 전무. 해결: 추적 중 타임아웃 붙은 PARTIAL_WAKE_LOCK |
| P0-4 | **환승 알람이 한 역 일찍 내리라고 안내** — 직전역 이름을 `transferStationName` 으로 넘김 → “내방역에서 내려 4호선으로 갈아타세요”(실제 환승역은 이수). AlarmScreen·TrackingScreen 의 “여기서 내려”도 동일 | `TrackingService.kt:250-254`, `alarm/NotificationHelper.kt:75`, `ui/screen/alarm/AlarmScreen.kt`, `ui/screen/tracking/TrackingScreen.kt` | 코드 확인. 환승역 = 현재 구간의 도착역(`session.destStatnId`)을 써야 함 |
| P0-5 | **보행 환승역이 출발/도착이면 출발=도착 구간 생성 → 크래시 루프** — 이수→사당 = `[7호선 이수→이수, 4호선 …]`. `directionBetween` 의 `require(p.size>=2)` 예외가 lifecycleScope 에서 미처리 → `START_REDELIVER_INTENT` 로 재시작마다 크래시 | `domain/usecase/JourneyPlannerUseCase.kt:75-93`, `domain/model/StationGraph.kt:99`, `TrackingService.kt:187` | 코드 추적 확인. 해결: 같은 역 구간 제거 + 루프에 예외 가드 |
| P0-6 | **목적지에 안 가는 열차를 자동 매칭** — 적합 후보가 없으면 `base` 로 폴백, 유일 후보면 확정. 예: 서울역→부평에서 천안행만 있으면 매칭 → 구로에서 갈라져 알람 안 울림. 2호선 지선 셔틀도 같은 경로로 오매칭 | `domain/usecase/MatchBoardedTrainUseCase.kt:69` | 코드 확인. 해결: 부적합 후보는 자동 확정 금지, 수동 선택 후보로만 노출 |
| P0-7 | **2호선 시청·충정로 출발 시 방향 판정 반전** — 순환을 닫는 간선(충정로 42 ↔ 시청 0) 때문에 `p[1].order >= p[0].order` 가 양방향 모두 같은 값 | `domain/model/StationGraph.kt:100` | 시드 order 확인(시청:0 … 충정로:42 + 닫는 간선). 해결: 순환 닫는 간선은 래핑으로 취급 |
| P0-8 | **실시간 미지원 노선(인천1·2, 김포, 의정부, 용인) 구간은 알람 불가** — `boardedTrainNo` 가 안 채워져 매번 매칭 분기로 가고 가속도 추정이 무시됨. 다음 구간으로도 못 넘어감 | `domain/usecase/TrackingStepUseCase.kt:54-56`, `TrackingService.kt:142-178` | 코드 확인. 해결: 미지원 노선은 탑승 확정을 수동/즉시로 처리하고 ACCEL 경로로 진입 |

### P1 — 오알람·크래시·신뢰성

| # | 결함 | 위치 |
|---|---|---|
| P1-1 | 가속도 추정 중복 카운트 — 이미 전진시킨 `currentStatnId` 에 누적 `stopCount` 를 매 폴링 다시 더함 → 정차 1회·폴링 3회면 3역 전진 → 조기 오알람 | `TrackingService.kt:209`, `domain/usecase/HybridCoordinator.kt` |
| P1-2 | 시드 구간 밖 역에 열차가 있으면 `remainingStops` 의 `require()` 가 예외 → 크래시 | `domain/model/StationGraph.kt:154`, `TrackingService.kt:244` |
| P1-3 | 알람 인텐트 extra 를 지우지 않아 회전·최근앱 복귀 시 유령 알람 화면 → “확인” 누르면 구간 건너뜀. `AdvanceJourney` 가 세션 ALERTED 여부를 안 봐서 비멱등 | `ui/MainActivity.kt`, `domain/usecase/AdvanceJourneyUseCase.kt:30-40` |
| P1-4 | 알람이 확인할 때까지 계속되지 않음 — 트리거 직후 서비스 종료(프로세스 보호 상실), 진동이 USAGE_ALARM 아님(절전 시 무시), 소리 1회 | `TrackingService.kt:258-259`, `alarm/VibratorController.kt`, `alarm/NotificationHelper.kt` |
| P1-5 | 잠금화면 위에 알람 화면이 안 뜸(`setShowWhenLocked`/`setTurnScreenOn` 없음), Android 14+ 풀스크린 인텐트 권한 확인 없음 | `ui/MainActivity.kt` |
| P1-6 | 지선 셔틀·V자 환승(까치산→홍대입구, 가산디지털단지→구일, 홍대입구→서울역 경의중앙)을 단일 구간으로 계산 | `domain/usecase/JourneyPlannerUseCase.kt` |
| P1-7 | 열차 확인·재매칭 버튼이 `startForegroundService` 를 쓰는데 해당 분기는 `startForeground` 를 안 부름 → 서비스가 안 떠 있을 때 누르면 크래시 가능 | `ui/screen/tracking/TrackingScreen.kt:84,95`, `TrackingService.kt:81-92` |
| P1-8 | 알림 권한 거부 시 아무 경고 없음(알람이 진동만 남음), 추적 시작 탭마다 권한 요청 | `ui/screen/home/HomeScreen.kt` |
| P1-9 | 노선 현황판이 백그라운드에서도 15초 폴링 → 공용 API 한도 소모 → 추적이 429 백오프 | `ui/screen/linemap/LineMapViewModel.kt:76-101` |
| P1-10 | Android 15 dataSync 일 6시간 한도 대비 없음(`onTimeout` 미구현) — 멈춘 세션이 한도 소진 | `TrackingService.kt` |

### 기능 업그레이드 후보 (검토 시 제안)
1. 알람 설정 화면 — `AlarmSetting`(몇 정거장 전·소리·진동·반복) 모델은 있으나 UI 와 서비스 배선이 없음(항상 기본값).
2. 급행 대응 — 매칭이 `isExpress` 를 안 봄. 목적지를 통과하는 급행에 탑승 매칭될 수 있음(9호선·1호선·경의중앙·수인분당). 급행 정차역 데이터 필요.
3. 진행 중 추적 복귀 — 추적 알림 탭/앱 재실행 시 추적 화면으로(지금은 홈).
4. 도착 예상 시간 — `avgSecToNext` 가 거의 전부 120초 고정이라 정밀 ETA 는 역간 소요시간 데이터 필요.
5. 즐겨찾기 원탭 시작(홈 위젯·빠른 설정 타일), 막차 표시(`isLastTrain` 미사용), 시드 구간 확장(3호선 일산선·4호선 과천/안산선).

---

## §A [완료 2026-07-16] 광역선 4개 간선(edge) 재작성

> **이 섹션은 완료됨.** 아래 설계대로 구현했고 전체 테스트 green.
> 실제 결과와 설계가 다른 점 1건: Acceptance 에 “김포공항→잠실 = 9호선→종합운동장→2호선”
> 이라 적었으나 실제로는 **공항철도[→홍대입구] ⇄ 2호선[→잠실]** 이 나온다.
> 홉 수가 더 적고(29 vs 32) 실사용상으로도 타당한 경로라 정상으로 판정했다.
> 아래 내용은 같은 유형의 사고가 재발했을 때 참고하도록 남겨둔다.

### A-0. 결론 먼저
`ExtraLinesSeed.kt` 의 **4개 노선만** `order` 가 지리 순서가 아니라 뒤죽박죽으로 부여돼 있고,
`edges` 가 그 잘못된 순서를 그대로 사슬로 연결한다. **역명·statnId·위경도는 전부 정확**하므로
**새 데이터 수집 없이 순서만 바로잡으면 된다.**

### A-1. 증거 (재현 방법)
그래프 실제 인접(`StationGraph.adjacent`)끼리의 위경도 거리를 재면:

| 노선 | 파손 간선 | 예시 |
|---|---|---|
| 경의중앙선 `1063` | 56개 중 **44개** | 도농↔운천 45km, 운천↔서강대 39km |
| 수인분당선 `1075` | 62개 중 **51개** | 사리↔서현 25km, 상갈↔어천 17km |
| 공항철도 `1065` | 13개 중 **11개** | 서울역↔인천공항1터미널 47km |
| 신분당선 `1077` | 15개 중 **10개** | 강남↔광교 21km |

정상 확인: 1~9호선, 경춘·우이신설·신림·서해·인천1·2·김포·의정부·용인.
오탐 주의(고치지 말 것): 1호선 연천–청산 13km, GTX-A 전 구간, 경강선 부발–세종대왕릉 8km,
인천2호선 아시아드경기장–서해구청 9km — **실제로 역간 거리가 먼 구간**이다.

재현 코드는 §A-6 감사 테스트를 먼저 추가하면 그대로 실패로 드러난다.

### A-2. 사용자 영향
1. **환승 경로 오계산** — 예: `김포공항→잠실` 이 `서해선(고양) ⇄ 경의중앙선(구리) ⇄ 8호선` 으로 나옴.
   정상은 `9호선 → 종합운동장 → 2호선`. 깨진 간선이 “지름길”처럼 보여 Dijkstra 가 그쪽을 고른다.
2. **추적 오동작** — 4개 노선 모두 `supportsRealtime=true` 라 실제 추적을 시도하는데,
   `nextStation`/`remainingStops`/`stationBeforeArrival` 이 전부 틀려 **엉뚱한 역에서 알람**이 울린다.

### A-3. 설계 (노선별 방침)

| 노선 | 분기 | 방침 |
|---|---|---|
| 수인분당선 `1075` (63역) | 없음 | `order` 를 지리 순서로 재부여 후 **`edges` 삭제(null)** → LINEAR 자동 인접 |
| 공항철도 `1065` (14역) | 없음 | 동일 — `edges` 삭제 |
| 신분당선 `1077` (16역) | 없음 | 동일 — `edges` 삭제 |
| 경의중앙선 `1063` (57역) | **있음**(가좌–신촌–서울역 지선) | `order` 재부여 + **`edges` 명시 유지**, `topology=BRANCHED` 로 변경 |

**분기 없는 3개는 `edges` 를 지우는 쪽이 안전하다** — `StationGraph` 는 `explicitEdges == null` 이면
order 정렬 인접으로 간선을 자동 생성하므로(`StationGraph.kt` init 블록), 사슬을 손으로 다시 쓰다가
어긋날 여지를 없앤다. 이미 1호선이 BRANCHED + 명시 엣지로 잘 돌고 있으니 경의중앙선은 그 패턴을 따른다.

### A-4. 정답 순서 (역명 기준, 이 순서대로 `order=0..N` 부여)

> **검증 완료(2026-07-16, Claude)**: 아래 4개 목록은 현재 시드의 역명 집합과 **완전 일치**함을
> 스크립트로 확인했다 — 오타 0, 누락 0, 중복 0, 개수 일치(16/14/63/57).
> 즉 **이름을 새로 만들 필요 없이 순서만 이대로 재배열하면 된다.**

**신분당선 `1077`** (16역, 북→남)
```
신사, 논현, 신논현, 강남, 양재(서초구청), 양재시민의숲, 청계산입구, 판교,
정자, 미금, 동천, 수지구청, 성복, 상현, 광교중앙, 광교
```

**공항철도 `1065`** (14역, 서울역→인천공항)
```
서울역, 공덕, 홍대입구, 디지털미디어시티, 마곡나루, 김포공항, 계양,
검암, 청라국제도시, 영종, 운서, 공항화물청사, 인천공항1터미널, 인천공항2터미널
```

**수인분당선 `1075`** (63역, 청량리→인천)
```
청량리(서울시립대입구), 왕십리, 서울숲, 압구정로데오, 강남구청, 선정릉, 선릉, 한티,
도곡, 구룡, 개포동, 대모산입구, 수서, 복정, 가천대, 태평, 모란, 야탑, 이매, 서현,
수내, 정자, 미금, 오리, 죽전, 보정, 구성, 신갈, 기흥, 상갈, 청명, 영통, 망포,
매탄권선, 수원시청, 매교, 수원, 고색, 오목천, 어천, 야목, 사리, 한대앞, 중앙, 고잔,
초지, 안산, 신길온천, 정왕, 오이도, 달월, 월곶, 소래포구, 인천논현, 호구포,
남동인더스파크, 원인재, 연수, 송도, 인하대, 숭의, 신포, 인천
```

**경의중앙선 `1063`** (57역 = 본선 55 + 지선 2, 임진강→지평)
```
본선(order 0..54):
임진강, 운천, 문산, 파주, 월롱, 금촌, 금릉, 운정, 야당, 탄현, 일산, 풍산, 백마,
곡산, 대곡, 능곡, 행신, 강매, 한국항공대, 수색, 디지털미디어시티, 가좌, 홍대입구,
서강대, 공덕, 효창공원앞, 용산, 이촌(국립중앙박물관), 서빙고, 한남, 옥수, 응봉,
왕십리, 청량리(서울시립대입구), 회기, 중랑, 상봉(시외버스터미널), 망우, 양원, 구리,
도농, 양정, 덕소, 도심, 팔당, 운길산, 양수, 신원, 국수, 아신, 오빈,
양평(경의중앙선), 원덕, 용문, 지평

지선(order 55..56): 신촌(경의중앙선), 서울역
지선 엣지: 가좌 ↔ 신촌(경의중앙선) ↔ 서울역   (가좌는 이웃 3개: 디지털미디어시티·홍대입구·신촌)
```
> `한국항공대` 는 구 화전역(시드 주석에 명시됨). `도심` 은 덕소–팔당 사이 실재 역.
> `신촌(경의중앙선)`·`양평(경의중앙선)` 은 2호선/5호선 동명이역과 구분하려고 이번 감사에서 개명한 이름 — **그대로 유지할 것**.

### A-5. 반드시 함께 확인할 것 — `directionPolarity`
`order` 를 재부여하면 **“order 증가 방향”의 의미가 바뀐다.** `StationGraph.directionBetween` 이
`p[1].order >= p[0].order → ASCENDING` 이고, `DirectionPolarity(ascendingUpdnLine)` 가 이 방향을
API `updnLine` 코드에 매핑하므로, **4개 노선 모두 극성을 실측 재검증해야 한다**(추정 금지 — 프로젝트 원칙).

검증 방법(실제 열차의 종착역 `statnTnm` 과 대조):
```bash
KEY=$(grep SEOUL_API_KEY local.properties | cut -d= -f2)
curl -s "http://swopenapi.seoul.go.kr/api/subway/$KEY/json/realtimePosition/1/500/신분당선" \
  | python3 -m json.tool | grep -E '"statnNm"|"updnLine"|"statnTnm"'
```
- `updnLine` 값과 `statnTnm`(종착역)을 짝지어, **order 가 커지는 종착역으로 가는 열차의 updnLine** 을
  `ascendingUpdnLine` 에 넣는다. 예) 신분당선에서 종착이 `광교`(order 15)면 그 열차의 updnLine 이 ASCENDING.
- 현재 4개 노선 모두 `DirectionPolarity("1")` 로 되어 있는데 **검증 흔적이 없다** — 재확인 대상.

### A-6. 재발 방지 테스트 (데이터 수정과 같은 커밋에 추가)
`app/src/test/java/com/jin/subwayalarm/data/local/SeedAdjacencyDistanceTest.kt` (신규)
- 모든 노선에 대해 `StationGraph(stations, edges).adjacent()` 로 **실제 인접쌍**을 구하고 위경도 거리 계산
- **8km 초과면 실패**. 단 아래는 실제 장거리라 화이트리스트:
  `1호선 연천–청산·전곡–소요산·평택–성환`, `GTX-A 전 구간`, `경강선 부발–세종대왕릉`,
  `인천2호선 아시아드경기장–서해구청`
- 기존 `SeedStationNameCollisionTest` 와 같은 형식(실패 메시지에 위반 쌍 전부 출력)으로 작성할 것.

### A-7. Acceptance
1. `./gradlew :app:testDebugUnitTest` green (신규 인접거리 테스트 포함)
2. `JourneyPlannerUseCase` 실측: `김포공항→잠실` 이 **9호선 → 종합운동장 → 2호선 잠실** 2구간으로 나온다
3. `수원→판교` 가 수인분당선 정자 환승 등 지리적으로 납득 가능한 경로
4. 4개 노선 `directionPolarity` 실측 근거를 커밋 메시지 또는 시드 주석에 남길 것
5. 기존 145+ 테스트 회귀 없음

### A-8. 손대는 파일
- `app/src/main/java/com/jin/subwayalarm/data/local/ExtraLinesSeed.kt` (**주 작업 대상**)
- `app/src/test/java/com/jin/subwayalarm/data/local/SeedAdjacencyDistanceTest.kt` (신규)
- (경의중앙선 한정) `LineMeta.topology` → `BRANCHED`

---

## 핵심 결정
| 결정 | 이유 | 대안(기각) |
|---|---|---|
| 보행 환승 = 별칭 테이블 | 공식 역명 유지 | 시드 역명 강제 통일 |
| LineMeta 확장 필드는 시드 병합 | Room 스키마 마이그레이션 회피 | Entity 컬럼 추가 |
| 후보 목록 = 인메모리 Repository | 사망 복구 시 다음 폴링 재계산 | TripSession Room 확장 |
| ACCEL 재판정은 추정 역 있을 때만 | 오발화 방지 | 소실 시 마지막 위치로 무조건 재판정 |
| Replay = TrackingStep 헤드리스 | 서비스 리팩터 없이 회귀 | TrackingService 주입 교체 먼저 |
| 동명이역은 좌표 600m 가드로 차단 | 신촌·양평처럼 이름만 같은 별개 역이 가짜 환승을 만듦 | 역명만 신뢰(사고 발생), 전역 역ID 도입(데이터 없음) |
| 분기 없는 노선은 `edges=null`(LINEAR) | 사슬을 손으로 쓰다 어긋날 여지 제거 | 전 노선 명시 엣지(오타 리스크) |
| 시드 정합성은 테스트로 고정 | 노선 추가 시 사람 검토에 의존하면 또 샘 | 문서 체크리스트만 유지 |
| statnId 는 경계에서 정규화 | 시드 ID 를 전부 실측 교정하려면 그 역에 열차가 있을 때만 알 수 있어 불완전 | 시드 ID 일괄 수정(관측 불가 역은 못 고침), 하위 로직마다 폴백(중복·누락 위험) |
| 역명 폴백은 3단계로 제한 | 괄호·공백·끝 “역” 까지만 무시하고, 정규화 충돌 시엔 추측하지 않음 | 유사도 매칭(오매칭 시 엉뚱한 역에서 알람) |

## 주요 신규/변경 경로
- `domain/model/WalkTransfers.kt`, `SubwayNetwork.kt`, `LineMeta.kt`, `TrainPosition.kt`, `BoardingCandidate.kt`
- `domain/usecase/MatchBoardedTrainUseCase.kt`, `TrackingStepUseCase.kt`, `ConfirmBoardedTrainUseCase.kt`
- `domain/usecase/AccelStopCounter.kt`, `HybridCoordinator.kt`
- `replay/ReplayFixture.kt`, `ReplayTripRunner.kt`
- `tracking/TrackingService.kt`, `tracking/sensor/LinearAccelerationSource.kt`
- `data/repository/BoardingCandidateRepository.kt`, `data/remote` DTO/mapper
- `ui/screen/tracking/*` 재매칭 UI
- 테스트: `WalkTransferTest`, `ReplayTripRunnerTest`, `AccelStopCounterTest`, `HybridCoordinatorTest` 등

## 주의사항
- **DB 스키마를 바꿀 때 `fallbackToDestructiveMigration()` 에 기대지 말 것.** v5 부터
  `favorite_route` 에 사용자가 저장한 즐겨찾기가 들어있고, 이건 재시드로 복구되지 않는다
  (역·노선 시드와 다름). 버전을 올릴 땐 `SubwayDatabase.MIGRATION_4_5` 처럼 실제
  Migration 을 만들어 `DatabaseModule.addMigrations(...)` 에 등록할 것.
  exportSchema=false 라 컴파일 타임 검증이 없으므로, **기기에 기존 버전이 깔린 상태에서
  덮어 설치해 Room 이 스키마 불일치 예외를 던지지 않는지 반드시 확인**해야 한다.
- 1호선 API `lineName` 은 여전히 `"1호선"` (표시만 displayName).
- FGS 타입 dataSync 유지 — 센서는 보조.
- OEM 배터리 최적화·실기기 센서 동작은 미검증.
- `AdvanceJourney` 환승 시 다음 구간 역명 매칭은 exact name 유지 (이수 환승 legs 는 board/alight 표기 다름 — 정상).
- **§A 작업 시 헛수고 방지 메모 (Claude 실측으로 이미 배제한 가설)**
  - “새 노선 시드가 희소해서 경로가 이상한 것” → **아님.** 역 수는 실제와 일치(수인분당 63, 경의중앙 57,
    공항철도 14, 신분당 16). 문제는 `order`/`edges` 뿐.
  - “`order` 기준 인접거리”로 감사하면 2·5·8호선·경춘선 등이 무더기로 걸리는데 **전부 오탐**이다.
    이 노선들은 `explicit edges` 를 쓰므로 order 인접은 의미가 없다. 반드시 `StationGraph.adjacent()`
    (엣지 반영)로 감사할 것.
  - “강남→교대 경로 없음” → **버그 아님.** 시드 역명이 `교대(법원.검찰청)` 이라 그렇고, 앱 드롭다운은
    전체 역명을 보여주므로 실사용에 문제 없음.
  - 좌표 600m 가드(`SubwayNetwork.isPhysicallySameStation`)는 현재 **아무 환승도 막고 있지 않다**
    (모든 동명 쌍이 600m 이내). 경로가 이상한 원인을 여기서 찾지 말 것.

## 환경 메모
- 테스트: `./gradlew :app:testDebugUnitTest`
- 단일 테스트 재실행(캐시 무시): `./gradlew :app:testDebugUnitTest --tests "*이름*" -i --rerun-tasks`
  — `-i --rerun-tasks` 없으면 `println` 출력이 안 보이고 UP-TO-DATE 로 건너뛴다.
- API 키: `local.properties` → `SEOUL_API_KEY` (gitignore 됨, 원격에 없음)
- 원격: `https://github.com/uijinp/subwayExit.git` (브랜치 `master`)
- 실기기 설치: `adb` 는 PATH 에 없다 → `export PATH="$PATH:$HOME/Library/Android/sdk/platform-tools"`
- `adb shell input text` 는 이 단말에서 **한글 입력 불가**(NPE) — UI 검증은 드롭다운 항목을 좌표로 탭할 것
