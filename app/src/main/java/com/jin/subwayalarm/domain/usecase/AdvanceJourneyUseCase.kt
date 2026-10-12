package com.jin.subwayalarm.domain.usecase

import android.content.Context
import androidx.core.content.ContextCompat
import com.jin.subwayalarm.alarm.AlarmController
import com.jin.subwayalarm.data.repository.JourneyPlanRepository
import com.jin.subwayalarm.data.repository.StationRepository
import com.jin.subwayalarm.data.repository.TripSessionRepository
import com.jin.subwayalarm.domain.model.TripSession
import com.jin.subwayalarm.domain.model.TripState
import com.jin.subwayalarm.tracking.TrackingService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 알람 "확인" 의 단일 진입점 (알림 버튼·알람 화면·추적 화면 배너가 모두 이걸 부른다).
 *
 * 다음 구간이 있으면: 다음 구간 TripSession 을 저장하고 TrackingService 를 다시 돌린다.
 * 마지막 구간이었으면: 여정을 끝내고 서비스를 멈춘다.
 *
 * **알람이 울린(ALERTED) 세션에서만 진행한다.** 예전엔 상태를 보지 않아, 알림에서 "해제"를
 * 누른 뒤 뒤에 남은 알람 화면에서 또 "확인"을 누르거나 회전으로 알람 화면이 다시 뜨면
 * 구간을 하나 더 건너뛰어 — 마지막 구간이면 최종 알람이 사라졌다.
 */
@Singleton
class AdvanceJourneyUseCase @Inject constructor(
    private val journeyPlanRepository: JourneyPlanRepository,
    private val tripSessionRepository: TripSessionRepository,
    private val stationRepository: StationRepository,
    private val alarmController: AlarmController,
    @ApplicationContext private val context: Context,
) {
    private val lock = Mutex()

    suspend operator fun invoke() = withContext(NonCancellable) {
        // 화면이 곧바로 넘어가며 호출측 scope 가 취소돼도, 계획·세션 저장이 반쯤 끝난 채로 남지 않게.
        lock.withLock { advance() }
    }

    private suspend fun advance() {
        alarmController.dismiss()

        val session = tripSessionRepository.current()
        if (session != null && session.state != TripState.ALERTED) return

        val plan = journeyPlanRepository.current()
        if (session == null || plan == null || !plan.hasNextLeg) {
            endJourney()
            return
        }

        val nextPlan = plan.copy(currentLegIndex = plan.currentLegIndex + 1)
        val leg = nextPlan.currentLeg
        val stations = stationRepository.stations(leg.lineId)
        val originId = stations.firstOrNull { it.name == leg.boardStationName }?.statnId
        val destId = stations.firstOrNull { it.name == leg.alightStationName }?.statnId
        if (originId == null || destId == null || originId == destId) {
            // 다음 구간 역 정보를 못 찾으면(데이터 불일치) 안전하게 여정을 종료한다.
            endJourney()
            return
        }

        journeyPlanRepository.save(nextPlan)
        tripSessionRepository.save(
            TripSession(
                originStatnId = originId,
                destStatnId = destId,
                lineId = leg.lineId,
                currentStatnId = originId,
                lastSyncAtEpochMillis = System.currentTimeMillis(),
            ),
        )
        ContextCompat.startForegroundService(context, TrackingService.startIntent(context))
    }

    private suspend fun endJourney() {
        journeyPlanRepository.clear()
        tripSessionRepository.clear()
        // 알람 뒤에도 서비스는 확인을 기다리며 떠 있으므로(프로세스 보호) 여기서 내린다.
        runCatching { context.startService(TrackingService.stopIntent(context)) }
    }
}
