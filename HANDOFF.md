# HANDOFF — subwayAlarm 전체 구현 (갭 + 데이터 감사)
> 최종 갱신: 2026-07-16 / by Grok

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
- [ ] **← 다음 작업**: 실기기 E2E — 7호선 이수 환승 경로 + 주머니 알람 + “다른 열차?” 수동 확인
- [ ] 이후: Replay JSON 픽스처를 `test/resources` 로 외부화(현재 코드 픽스처)
- [ ] 이후: 가속도 임계값 실기기 튜닝 (A9)

## 핵심 결정
| 결정 | 이유 | 대안(기각) |
|---|---|---|
| 보행 환승 = 별칭 테이블 | 공식 역명 유지 | 시드 역명 강제 통일 |
| LineMeta 확장 필드는 시드 병합 | Room 스키마 마이그레이션 회피 | Entity 컬럼 추가 |
| 후보 목록 = 인메모리 Repository | 사망 복구 시 다음 폴링 재계산 | TripSession Room 확장 |
| ACCEL 재판정은 추정 역 있을 때만 | 오발화 방지 | 소실 시 마지막 위치로 무조건 재판정 |
| Replay = TrackingStep 헤드리스 | 서비스 리팩터 없이 회귀 | TrackingService 주입 교체 먼저 |

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
- 1호선 API `lineName` 은 여전히 `"1호선"` (표시만 displayName).
- FGS 타입 dataSync 유지 — 센서는 보조.
- OEM 배터리 최적화·실기기 센서 동작은 미검증.
- `AdvanceJourney` 환승 시 다음 구간 역명 매칭은 exact name 유지 (이수 환승 legs 는 board/alight 표기 다름 — 정상).

## 환경 메모
- 테스트: `./gradlew :app:testDebugUnitTest`
- API 키: `local.properties` → `SEOUL_API_KEY`
