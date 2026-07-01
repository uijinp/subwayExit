package com.jin.subwayalarm.domain.usecase

import android.content.Context
import androidx.core.content.ContextCompat
import com.jin.subwayalarm.alarm.AlarmController
import com.jin.subwayalarm.data.repository.JourneyPlanRepository
import com.jin.subwayalarm.data.repository.StationRepository
import com.jin.subwayalarm.data.repository.TripSessionRepository
import com.jin.subwayalarm.domain.model.TripSession
import com.jin.subwayalarm.tracking.TrackingService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 알람 확인/해제 시 호출되는 단일 오케스트레이션 지점 (AlarmDismissReceiver·TrackingViewModel·
 * AlarmViewModel 전부 이걸 통해서만 여정을 진행/종료한다).
 *
 * 다음 구간이 있으면: 다음 구간용 TripSession 을 새로 저장하고 TrackingService 를 재시작.
 * 마지막 구간이었으면: 여정 전체 종료(오늘까지의 동작과 동일).
 */
@Singleton
class AdvanceJourneyUseCase @Inject constructor(
    private val journeyPlanRepository: JourneyPlanRepository,
    private val tripSessionRepository: TripSessionRepository,
    private val stationRepository: StationRepository,
    private val alarmController: AlarmController,
    @ApplicationContext private val context: Context,
) {
    suspend operator fun invoke() {
        alarmController.dismiss()

        val plan = journeyPlanRepository.current()
        if (plan == null || !plan.hasNextLeg) {
            journeyPlanRepository.clear()
            tripSessionRepository.clear()
            return
        }

        val nextPlan = plan.copy(currentLegIndex = plan.currentLegIndex + 1)
        val leg = nextPlan.currentLeg
        val stations = stationRepository.stations(leg.lineId)
        val originId = stations.firstOrNull { it.name == leg.boardStationName }?.statnId
        val destId = stations.firstOrNull { it.name == leg.alightStationName }?.statnId
        if (originId == null || destId == null) {
            // 다음 구간 역 정보를 못 찾으면(데이터 불일치) 안전하게 여정을 종료한다.
            journeyPlanRepository.clear()
            tripSessionRepository.clear()
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
}
