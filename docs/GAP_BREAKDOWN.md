# 미구현 갭 작업 분해 — Replay / 재매칭 / 가속도계

> 작성: 2026-07-16 / 기준 코드: `master` @ `68210b2`  
> 설계 원본: `docs/DESIGN.md` §3·§8 작업 4·10·11  
> 목표: **구현 착수 가능한 크기**로 쪼개고, 의존 순서·Acceptance·건드리는 파일을 고정한다.

---

## 0. 공통 원칙

| 원칙 | 내용 |
|---|---|
| 순수 로직 우선 | 판정·매칭·카운터는 `domain/` 순수함수 + 단위테스트. FGS/센서는 어댑터. |
| 기존 틱 계약 유지 | `TrackingStepUseCase` 가 세션 전이 SoT. 하이브리드·재매칭은 이 계약을 확장. |
| Replay 먼저 | 단말·실열차 없이도 재매칭·ACCEL 시나리오를 고정할 수 있어야 회귀가 가능. |
| 한 역 전 목표 불변 | 늦느니 일찍(1-2). 센서·재매칭 실패 시에도 **오발화보다 미발화가 더 위험**하진 않지만, 규칙 C(만회)는 유지. |

### 권장 구현 순서

```
R0  Replay 기반 골격  →  R1  녹화 포맷·로더  →  R2  디버그 재생 UI
              ↓
M1  재매칭 도메인     →  M2  TrackingStep 연동  →  M3  UI
              ↓
A1  정차 상태기계     →  A2  HybridCoordinator   →  A3  센서 어댑터·FGS
```

- **Replay(R)** 가 없으면 M/A 의 통합 검증이 실기기 의존이 됨.  
- **재매칭(M)** 은 ACCEL 없이도 탑승 오매칭 리스크를 즉시 줄임.  
- **가속도계(A)** 는 API 소실 구간용 2차 백업(설계 3-4·작업11).

---

## 1. Replay 하버스 (설계 작업 4 — 1차 잔여 유일 항목)

### 1-1. 문제
Acceptance(§9)의 “단말 없이 반복”이 불가능.  
현재 단위테스트는 **한 틱 입력**만 검증하고, **시계열 폴링 루프·백오프·매칭 창**은 검증하지 못함.

### 1-2. 목표
녹화된 `realtimePosition` 시계열을 시간압축 재생해  
`TrackingStepUseCase` 루프(또는 경량 하버스)로 **MATCHING → TRACKING → ALERTED** 를 재현.

### 1-3. 작업 분해

| ID | 작업 | 산출물 | 예상 공수 | 의존 |
|---|---|---|---|---|
| **R0** | 패키지·계약 정의 | `replay/` 패키지, `ReplayFixture` 스키마 | 0.5d | — |
| **R1** | JSON 픽스처 포맷 + 로더 | `assets/replay/*.json` 또는 `test/resources/replay/` | 1d | R0 |
| **R2** | 시간축 플레이어 | `ReplayClock`, `ReplayPositionSource` | 1d | R1 |
| **R3** | 헤드리스 러너 | `ReplayTripRunner` — 세션 전이 로그 + 최종 Trigger 단언 | 1d | R2 |
| **R4** | 시나리오 3종 고정 | 정상 / 매칭 지연 / 소실 후 재등장 | 1d | R3 |
| **R5** | (옵션) 디버그 화면 | 개발 빌드에서 픽스처 선택·배속 재생 | 1d | R3 |
| **R6** | (옵션) 실응답 녹화 도구 | 디버그 메뉴 “현재 노선 60초 덤프” | 0.5d | R1 |

### 1-4. 제안 스키마 (R0/R1)

```json
{
  "meta": {
    "lineName": "7호선",
    "lineId": "1007",
    "recordedAt": "2026-07-01T12:00:00+09:00",
    "description": "건대입구→고속터미널, trainNo=7123"
  },
  "trip": {
    "originStatnId": "1007000727",
    "destStatnId": "1007000734",
    "leadStations": 1
  },
  "frames": [
    {
      "tOffsetSec": 0,
      "httpStatus": 200,
      "resultCode": "INFO-000",
      "positions": [ { "trainNo": "7123", "statnId": "...", "statnNm": "...", "updnLine": "1", "trainSttus": "1", "recptnDt": "..." } ]
    }
  ],
  "expect": {
    "finalState": "ALERTED",
    "alertRule": "AT_OR_ARRIVING_ALERT_STATION",
    "boardedTrainNo": "7123"
  }
}
```

- DTO 필드는 `TrainPositionDto` / `toDomainOrNull` 과 동일 키를 유지해 **실응답 덤프를 최소 변환**으로 넣는다.  
- `tOffsetSec` 기준 재생 + 테스트에서는 `ReplayClock` 이 `nowEpochMillis` 를 공급 (`TrackingStepInput.nowEpochMillis`).

### 1-5. 건드릴 파일 (신규 중심)

```
app/src/main/java/com/jin/subwayalarm/replay/
  ReplayFixture.kt          # 스키마 모델
  ReplayFixtureLoader.kt    # Moshi 로드
  ReplayClock.kt            # 가상 시각
  ReplayPositionSource.kt   # TrainPositionRepository 대체 또는 테스트 더블
  ReplayTripRunner.kt       # 루프 (서비스 없이)

app/src/test/resources/replay/
  line7_normal_alert.json
  line7_matching_window.json
  line7_train_missing.json

app/src/test/java/.../replay/ReplayTripRunnerTest.kt
```

**연동 전략 (권장):**  
`TrackingService` 를 바로 리팩터하지 말고, 먼저 **R3 헤드리스 러너가 `TrackingStepUseCase` 를 직접 호출**.  
서비스 주입 교체(`TrainPositionRepository` 인터페이스 구현체 스왑)는 R5 이후.

### 1-6. Acceptance

- [ ] 픽스처 1개로 직전역 Trigger 재현 (단말 없음)  
- [ ] 매칭 전 15s 고정 주기 가정이 러너에 반영되거나, 프레임 간격으로 대체 가능  
- [ ] 열차 소실 프레임에서 `mode=ACCEL` 유지·알람 미발화 후, 재등장 시 재판정  
- [ ] CI(`./gradlew test`) 에 Replay 시나리오 포함  

### 1-7. 비범위
- UI 애니메이션 동기화  
- 전 노선 자동 녹화 파이프라인  
- 백오프/429 시뮬은 프레임에 `httpStatus:429` 넣으면 충분 (별도 목 서버 불필요)

---

## 2. 수동 재매칭 (설계 3-3 “다른 열차?” + 작업 10 일부)

### 2-1. 문제
`MatchBoardedTrainUseCase` 는 **후보 정확히 1대**일 때만 확정.  
- 0대: MATCHING 유지 (15s 폴링 — 최근 수정)  
- 2대+: `Ambiguous` → 사용자 개입 없음 → **영구 MATCHING 가능**  
- 오매칭(다른 열차 탑승): 수정 UI 없음  

설계 최대 리스크 = 탑승 오매칭. **ACCEL 없이도 UI+도메인만으로 완화 가능.**

### 2-2. 목표
1. MATCHING 중 후보 N 표시 → 사용자 선택  
2. TRACKING 중 “다른 열차?” → 후보 다시 받아 재선택  
3. 선택 즉시 `boardedTrainNo` 교체·상태 `TRACKING`

### 2-3. 작업 분해

| ID | 작업 | 상세 | 예상 | 의존 |
|---|---|---|---|---|
| **M0** | 후보 스냅샷 모델 | `BoardingCandidate(trainNo, statnName, status, direction, recptnDt)` | 0.25d | — |
| **M1** | 후보 나열 UseCase | `ListBoardingCandidatesUseCase` — 기존 필터를 리스트 반환으로 분리 (`MatchBoardedTrain` 재사용) | 0.5d | M0 |
| **M2** | 확정 명령 | `ConfirmBoardedTrainUseCase(session, trainNo) → TripSession` 순수 전이 | 0.25d | M0 |
| **M3** | TrackingStep 연동 | `Ambiguous` 시 후보를 세션/사이드채널에 노출; 자동 확정은 1대일 때만 유지 | 0.5d | M1 |
| **M4** | Repository 관측 | 후보 목록을 `StateFlow` 또는 세션 확장 필드로 UI에 전달 | 0.5d | M3 |
| **M5** | TrackingScreen UI | 후보 칩/리스트 + “다른 열차?” 버튼 + 확정 | 1d | M4 |
| **M6** | 단위테스트 | Ambiguous·수동확정·TRACKING 중 재선택 | 0.5d | M2–M3 |
| **M7** | Replay 시나리오 | 후보 2대 프레임 → 수동 확정 후 알람 (R4 확장) | 0.5d | R3, M3 |

### 2-4. 세션 모델 확장안

**안 A (권장, 최소 침습):** 후보를 세션에 넣지 않고  
`TrackingService` 또는 전용 `BoardingCandidateRepository` 가 마지막 폴링 후보를 홀드.  
UI는 `TrackingViewModel` 이 관측.

**안 B:** `TripSession` 에 `candidateTrainNos: List<String>?` 추가 → Room 마이그레이션(v5).  
사망 복구 시 후보까지 복원 가능하나 스키마 비용 있음.

권장: **안 A로 MVP**, 복구 시 다음 폴링에서 후보 재계산.

### 2-5. 건드릴 파일

```
domain/usecase/MatchBoardedTrainUseCase.kt     # 필터 로직 공유 추출
domain/usecase/ListBoardingCandidatesUseCase.kt  # 신규
domain/usecase/ConfirmBoardedTrainUseCase.kt     # 신규
domain/usecase/TrackingStepUseCase.kt            # Ambiguous 처리 명시
tracking/TrackingService.kt                      # 후보 홀드 / 확정 인텐트
ui/screen/tracking/TrackingViewModel.kt
ui/screen/tracking/TrackingScreen.kt
data/local/entity/TripSessionEntity.kt           # 안 B일 때만
```

### 2-6. UX 스케치

| 상태 | UI |
|---|---|
| MATCHING + 0 후보 | “출발역에서 열차를 찾는 중…” |
| MATCHING + 1 후보 | 자동 확정 (현행) 또는 “7123 탑승으로 시작” 확인 1탭 |
| MATCHING + N 후보 | 리스트 선택 필수 |
| TRACKING | 앱바/하단 “다른 열차?” → 동일 후보 UI, 선택 시 trainNo 교체 |
| 확정 직후 | `currentStatnId` 를 해당 열차 보정 위치로 갱신 권장 (점프 허용) |

### 2-7. Acceptance

- [ ] 후보 2대 이상에서 자동 확정하지 않음  
- [ ] 사용자 선택 1회 후 TRACKING + 이후 폴링이 그 trainNo 만 추적  
- [ ] TRACKING 중 재선택 시 이전 trainNo 폐기, alerted 전제 초기화  
- [ ] 서비스 재시작 후에도 (다음 폴링으로) 추적 유지  

### 2-8. 비범위 (작업 10 나머지)
- 가속도계 정차 패턴으로 다중 후보 **자동 수렴** → A 단계와 결합  
- 실시간 도착 API(`realtimeStationArrival`) 보조 매칭  

---

## 3. 가속도계 백업 + HybridCoordinator (설계 3-4·작업 11)

### 3-1. 문제
`TrackingStepUseCase.trackBoarded` 에서 열차 소실 시:

```kotlin
// mode 만 ACCEL, 알람 재판정 스킵 — 위치 고정
return TrackingStepResult(session.copy(mode = TrackingMode.ACCEL), null)
```

지하 장구간·통신 실패가 **직전역 창과 겹치면 알람 실패**.  
UI는 “추정” 표시만 하고 추정 로직은 없음.

### 3-2. 목표
1. `TYPE_LINEAR_ACCELERATION` 기반 정차 카운터 (+1 역)  
2. API 생존 시 `resync(apiStatn)` 으로 오차 리셋  
3. API 정상 구간 센서 일시정지 (배터리)  
4. ACCEL 모드에서도 `EvaluateAlertUseCase` 재판정  

### 3-3. 작업 분해

| ID | 작업 | 상세 | 예상 | 의존 |
|---|---|---|---|---|
| **A0** | 상태기계 스펙 고정 | MOVING→DECEL→STOPPED→ACCEL, 임계값 표 | 0.5d | — |
| **A1** | 순수 카운터 | `AccelStopCounter` (샘플 시퀀스 in → 정차 이벤트 out) | 1.5d | A0 |
| **A2** | 역 인덱스 추정 | `estimate(graph, lastStatnId, direction, stopCount)` | 0.5d | A1 |
| **A3** | HybridCoordinator | API hit → resync+센서 pause / miss → 센서 resume+estimate | 1d | A2, R3 권장 |
| **A4** | TrackingStep 확장 | `TrackingStepInput` 에 `accelEstimate: String?` 또는 coordinator 결과를 입력 | 1d | A3 |
| **A5** | 센서 어댑터 | `LinearAccelSensor` 50Hz, 코루틴 Flow, 라이프사이클 | 1d | A1 |
| **A6** | FGS 연동 | `TrackingService` 가 센서 start/stop, 권한 불필요(신체 센서 일반) | 1d | A5, A4 |
| **A7** | 단위테스트 | 합성 가속 파형으로 정차 3회 → +3역; resync 후 카운터 0 | 1d | A1–A3 |
| **A8** | Replay+합성 하이브리드 | API 프레임 공백 구간에 가짜 정차 이벤트 주입 | 1d | R3, A3 |
| **A9** | 실기기 튜닝 | 7호선 지하 구간 1–2회 탑승 로그 | 1–2d | A6 |

### 3-4. 상태기계 초안 (A0 — 구현 전 튜닝 대상)

| 상태 | 진입 조건 (초안) | 비고 |
|---|---|---|
| MOVING | \|a\| 이동 평균 > A_move 지속 T_move | 주행 |
| DECEL | MOVING 중 감속 패턴 | 노이즈 가드 |
| STOPPED | \|a\| < A_stop 지속 T_stop (예: 8–15s) | **+1 정차** 커밋 |
| ACCEL | STOPPED 후 재가속 | MOVING 복귀 |

- 저역통과(예: 1–2Hz) 후 임계.  
- 정차 커밋은 **한 번만** (STOPPED 진입 엣지).  
- `resync(statnId)`: 내부 stopCount·current 강제 정렬, 상태 MOVING 리셋.

### 3-5. TrackingStep 계약 확장안

```kotlin
// 의사코드
if (train != null) {
  accel.resync(corrected.statnId)   // 서비스 측
  // 기존 API 경로 + evaluateAlert
} else {
  val estimatedId = accel.estimate() // 서비스 → input
  session' = session.copy(currentStatnId = estimatedId, mode = ACCEL)
  evaluateAlert(...)  // 스킵하지 않음
}
```

**중요:** 순수 UseCase 안에 Android Sensor 를 넣지 말 것.  
센서는 `tracking/` 또는 `sensor/`, UseCase 는 `estimatedStatnId` 만 수신.

### 3-6. 건드릴 파일

```
domain/usecase/AccelStopCounter.kt          # 순수 (또는 domain/tracking/)
domain/usecase/HybridCoordinator.kt         # 순수 정책
domain/usecase/TrackingStepUseCase.kt       # ACCEL 재판정
tracking/sensor/LinearAccelerationSource.kt
tracking/TrackingService.kt
AndroidManifest.xml                         # 센서 피처 선언은 optional
ui/.../TrackingScreen.kt                    # 추정 모드 배지 강화
```

### 3-7. 권한·플랫폼
- 가속도계: 런타임 권한 불필요 (단, 일부 OEM 배터리 최적화)  
- FGS 타입 변경 **불필요** (`dataSync` 유지 — 센서는 보조)  
- 백그라운드 센서: FGS 중이라 수신 가능 전제 (실기기 확인 A9)

### 3-8. Acceptance

- [ ] API 정상 시 센서 샘플링 중단(또는 최저 듀티)  
- [ ] API 소실 N정차 후 `currentStatnId` 가 진행 방향으로 증가/감소  
- [ ] API 복귀 시 카운터·현재역이 API 보정 위치로 정렬  
- [ ] ACCEL 만으로도 직전역 규칙 A/B/C 중 하나로 Trigger 가능  
- [ ] 오탐 정차(신호대기 수준의 짧은 정차)로 2역 이상 점프하지 않음 (튜닝)

### 3-9. 리스크
| 리스크 | 완화 |
|---|---|
| 정차 오탐/미탐 | 임계 보수적 + API resync 가 주 | 
| 환승 구간 도보 진동 | ACCEL 은 **탑승 중 leg** 에만, 환승 대기 시 센서 pause |
| 배터리 | API 모드 센서 off |
| 테스트 어려움 | A1 순수 파형 테스트 + A8 Replay 공백 |

---

## 4. 크로스컷·우선순위 백로그

### P0 (신뢰성 클로저)
1. **R0–R4** Replay 헤드리스  
2. **M1–M6** 수동 재매칭  
3. 데이터 감사 수정: **이수↔총신대 환승 별칭** (`docs/DATA_AUDIT.md` D-1)

### P1
4. **A0–A7** 가속도계 순수+하이브리드+FGS  
5. **R5–R6** 녹화 UX  
6. **M7** Replay×재매칭  

### P2
7. 다중 후보 자동 수렴 (ACCEL 패턴 매칭)  
8. `realtimeStationArrival` 보조  
9. 역간 실측 `avgSecToNext`  

### 의존 그래프

```
DATA-AUDIT D-1 (이수 별칭) ── 독립, 즉시 가능
R0→R1→R2→R3→R4
         └──────────→ M7, A8
M0→M1→M2→M3→M4→M5→M6
A0→A1→A2→A3→A4→A5→A6→A7→A9
              └→ A8 (needs R3)
```

### 공수 러프 합계
| 트랙 | 합계 |
|---|---|
| Replay R0–R4 | ~4.5d |
| 재매칭 M0–M6 | ~3.5d |
| 가속도계 A0–A7 | ~7.5d |
| 여유·실기기 | ~3d |
| **합 (P0+P1 코어)** | **~18–20d** |

---

## 5. 착수 시 첫 PR 제안

**PR-1 (가장 작음, 가치 큼):** 이수↔총신대 환승 별칭 + 테스트  
→ `docs/DATA_AUDIT.md` D-1  

**PR-2:** Replay 스키마 + 로더 + 빈 러너 + 픽스처 1개 스켈레톤 (R0–R1)  

**PR-3:** `ListBoardingCandidates` + Tracking UI 후보 선택 (M1–M5, 자동 매칭 동작 유지)

이 세 PR 이후 하이브리드(A)에 들어가도 회귀 기반이 생긴다.
