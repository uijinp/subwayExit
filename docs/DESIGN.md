# 지하철 하차 알람 앱 — 설계 명세서 v2

> 목적지를 입력하면 실시간 열차 위치 추적으로 **목적지 한 역 전**에 진동·알람.
> v1 설계 검토 반영본. 구현 중 충돌 시 각 결정의 **이유**를 우선한다.

## 0. 메타

| 항목 | 값 |
|---|---|
| 플랫폼 / 스택 | Android 네이티브 · Kotlin + Compose |
| 최소 / 타겟 SDK | API 26 / API 35 |
| **FGS 타입** | **`dataSync`** (위치 아님 — C-1) |
| **위치 권한** | 사용 안 함 (추적은 네트워크 폴링 기반) |
| **MVP 타깃 노선** | **7호선** (장암–석남, 순환·지선 없는 직선 단방향 — C-3) |
| 추적 방식 | 실시간 API(메인) + 가속도계(2차 백업) 하이브리드 |
| 테스트 기반 | Replay 하버스(녹화 응답 재생)를 1급 기능 |
| 배포(MVP) | 사이드로드 → 검증 후 스토어 |

## 1. 핵심 설계 결정 (이유 우선)

- **1-1. 순수 GPS 폐기.** 지하 구간 GPS 불안정 → SoT는 실시간 열차 위치 API.
- **1-2. 목표는 "도착 정확도"가 아니라 "한 역 전 트리거".** 수십 초 시차는 안전 마진으로 흡수.
- **1-3. 최난제는 추적이 아니라 "탑승 열차 특정".** 출발 시각 윈도우 + (2차)가속도계 검증.
- **1-4. 하이브리드의 본질 = re-sync.** API 생존 시 정차 카운터 보정 → 지하에서도 오차 미누적.
- **1-5. 추적은 네트워크 작업 → 위치 FGS 아님.** `dataSync` 타입 + 위치 권한 제거.
- **1-6. 역 위상은 배열 인덱스가 아니라 방향 그래프.** 순환·지선 대비, 직전역은 그래프 탐색.

## 2. 외부 API

> 모든 엔드포인트/필드는 설계 기준값. 착수 시 최신 명세로 재검증(작업 3 스파이크에서 강제).

- **인증:** `data.seoul.go.kr` OPEN API KEY → `local.properties` → `BuildConfig` 주입. 하드코딩 금지.
  일일 호출 한도(무료 ~1,000건/일) 존재 → 폴링 설계의 1급 제약.
- **2-2 실시간 위치(메인):** `http://swopenapi.seoul.go.kr/api/subway/{KEY}/json/realtimePosition/{S}/{E}/{노선명}`
  필드: `subwayId` `trainNo`(탑승 식별 키) `statnNm` `statnId` `updnLine`(0/1) `trainSttus`(0진입·1도착·2출발·3전역출발) `directAt` `lstcarAt` `recptnDt`.
  **cleartext(HTTP)** → `network_security_config.xml` 예외 필수(C-2).
- **2-3 실시간 도착(매칭 보조):** `.../realtimeStationArrival/{S}/{E}/{역명}` — `trainNo`·방향·`barvlDt`·`arvlMsg2/3`·`arvlCd`.
- **2-4 정적 메타(Room):** 역 순서·`statnId↔순번`·방향. **역간 평균 소요시간(초)** 컬럼 추가.
- **2-5 시간표 fallback / 미지원 구간(2차):** 서울 외(광명·서동탄·춘천 등).
- **2-6 recptnDt 보정:** `advance = floor(lag / 역간평균소요)`. 상수 휴리스틱 폐기.
  규칙: 항상 "더 빠른 알람" 방향으로만 올림.
- **2-7 폴링·레이트리밋:** 거리 적응형(>4역 75s / 2~4역 30s / ≤2역 15s). 429/한도 → 지수 백오프 + "갱신 지연" 표시.

## 3. 핵심 알고리즘

### 3-1. 직전역 판정 (그래프 기반)
```
alertStn = graph.prev(dest, 방향)          # leadStations=1
판정(둘 중 먼저):
  A) cur == alertStn AND trainSttus in {0,1}
  B) cur == graph.prev(alertStn, 방향) AND trainSttus == 2     # 보수적 조기
가드: 종착/회차/지선 분기, dest=시발역(alertStn 없음), 1회 트리거 후 alerted=true
```

### 3-2. 추적 루프 (FGS dataSync)
```
loop (적응형 주기):
  if 네트워크 && 실시간지원:
    train = fetchRealtimePosition(노선).firstOrNull { trainNo == session.boardedTrainNo }
    if train != null: cur = recptnDt보정(train.statnNm); accelCounter.resync(cur); mode=API
    else: mode=ACCEL
  else: mode=ACCEL
  if mode==ACCEL: cur = accelCounter.estimate()
  if 직전역판정(cur): triggerAlert(); stopSelf()
  persist(session)   # 사망 복구
```

### 3-3. 탑승 열차 특정
- MVP: "출발역 탑승 버튼 = 직후 방향 일치 출발 열차" → `boardedTrainNo` 확정.
- 2차: 후보 N개 → 가속도계 정차 패턴 최일치로 수렴 / 실패 시 사용자 선택.
- 상시: "다른 열차?" 수동 재매칭 버튼.

### 3-4. 가속도계 카운팅 (2차)
`TYPE_LINEAR_ACCELERATION` 50Hz, 상태기계(MOVING→DECEL→STOPPED→ACCEL), 정차 지속 임계로 +1, 저역통과 노이즈 가드, `resync(apiStatn)` 강제 정렬. API 정상 구간엔 센서 일시정지(배터리).

## 4. Android 구현

- **4-1 FGS:** type `dataSync`, `startForeground` + 상시 알림(현재역·정지 버튼), `START_REDELIVER_INTENT` 사망 복구, Doze 예외 안내(권장).
- **4-2 권한:** `INTERNET`, `FOREGROUND_SERVICE(+DATA_SYNC)`, `POST_NOTIFICATIONS`, `VIBRATE`, (`USE_FULL_SCREEN_INTENT` 조건부). **위치 권한 없음.**
- **4-3 알람:** 기본 = 하이프라이어티 알림 + `VibrationEffect.createWaveform` 강진동 + (옵션)소리. 풀스크린은 best-effort(스토어 심사 리스크).
- **4-4 네트워크 보안:** `network_security_config.xml` 에 `swopenapi.seoul.go.kr` 만 cleartext 허용.

## 5. 데이터 모델 (Room)
```
Station(lineId, statnId, statnName, orderInLine, updnLine, nextStatnId, prevStatnId, avgSecToNext)
LineMeta(lineId, lineName, supportsRealtime, topology: LINEAR|LOOP|BRANCHED)
TripSession(originStatnId, destStatnId, lineId, direction, boardedTrainNo?,
            currentStatnId, lastSyncAt, mode: API|ACCEL,
            state: SELECTING|MATCHING|TRACKING|ALERTED|DONE)   # 사망 복구 SoT
AlarmSetting(vibratePattern, sound, repeat, leadStations=1)
```

## 6. 화면 흐름 (MVP)
```
[홈] 노선(7호선)→출발역→목적지역→"추적 시작"
  → [탑승 확인] 단순가정 자동통과(2차: 다중후보 선택)
  → [추적 중] 추정 현재역·남은 역수·모드(API/ACCEL)·"다른 열차?"·정지
  → [알람] 직전역 도달 → 강진동·알람 → 해제
엣지: 여정 중간 진입(이미 탑승) → 현재역부터 시작
```

## 7. 패키지 구조
```
com.jin.subwayalarm
├── data/{remote(Retrofit·DTO·recptnDt보정·백오프), local(Room), repository}
├── domain/{model, usecase(MatchTrain, EvaluateAlert, CorrectPosition)}
├── tracking/{TrackingService, RealtimeTracker, AccelStopCounter(2차), HybridCoordinator(2차)}
├── alarm/{Vibrator, NotificationHelper}
├── replay/   # 녹화 응답 재생 하버스(테스트·디버그)
└── ui/{Compose, ViewModel}
```

## 8. 작업 순서
1. **셋업** — 골격 + network_security_config + BuildConfig 키 주입 + FGS dataSync 스텁. ✅
2. **정적 데이터** — 노선·역 메타 동기화 → Room. 그래프 위상·역간소요 적재 + 방향/직전역 유틸 + 단위테스트.
3. **API 실측 검증 스파이크** — 7호선 1~2h 응답 덤프로 trainNo 안정성·trainSttus 코드·필드명 검증 후 DTO 확정.
4. **Replay 하버스** — 덤프 시간압축 재생. 이후 모든 로직 단말 없이 검증.
5. **실시간 연동** — realtimePosition 파싱 + recptnDt 보정 + 적응형 폴링·백오프 + 테스트.
6. **직전역 판정** — EvaluateAlertUseCase 순수함수 + 엣지 단위테스트(종착·시발역·소실).
7. **FGS + 추적 루프(API 단독)** — 폴링·현재역 갱신·사망 복구·알림. *MVP 핵심.*
8. **알람·진동** — 강진동·하이프라이어티 알림·해제 UX.
9. **UI** — 홈/선택/추적중(+재매칭)/알람.
10. (2차) 다중 후보 매칭 수렴.
11. (2차) 가속도계 백업 + HybridCoordinator.
12. (2차) 시간표 fallback / 순환·지선 노선 확장(2호선 등).

## 9. Acceptance
- [ ] Replay 재생으로 7호선 임의 구간 → 직전역 트리거(단말 없이 반복).
- [ ] 실기기 7호선 → 직전역 진동·알람.
- [ ] 화면 끄고 주머니 상태에서도 알람(FGS).
- [ ] recptnDt 시차 큰 응답에서도 타이밍 늦지 않음(보수적 조기).
- [ ] 종착·시발역·열차 소실 입력 시 크래시·오발화 없음.
- [ ] FGS 강제 종료 후 TripSession 으로 추적 재개.
- [ ] API 한도/429 시 백오프·"갱신 지연", 크래시 없음.
- [ ] (2차) 지하 소실 시 가속도계 유지, API 복귀 시 resync.

## 10. 결정사항
| ID | 항목 | 결정 |
|---|---|---|
| D-1 | 스택 | Kotlin 네이티브 |
| D-2/3 | 위치·GPS | 추적엔 불사용. FGS=dataSync |
| D-4 | 다중 매칭 | 2차 이연 |
| D-5 | 타깃 노선 | **7호선(직선)**. 2호선은 작업12 |
| D-6 | 배포 | MVP 사이드로드 → 검증 후 스토어 |

## 11. 리스크
- **탑승 오매칭(최대 리스크)** — MVP 단순가정 + "다른 열차?" 보정, 2차 수렴으로 해소.
- **API 한도·시차** — 적응형 폴링·백오프. "한 역 전"엔 정밀도 충분.
- **OEM 배터리 종료** — FGS + 사망 복구 + 배터리 예외 안내.
- **dataSync 6h 캡(Android 15)** — 단일 여정 무관, 장시간 미해제 세션만 주의.
