package com.jin.subwayalarm.ui.screen.alarm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jin.subwayalarm.data.repository.AlarmSettingsRepository
import com.jin.subwayalarm.data.repository.JourneyPlanRepository
import com.jin.subwayalarm.data.repository.StationRepository
import com.jin.subwayalarm.data.repository.TripSessionRepository
import com.jin.subwayalarm.domain.model.AlarmSetting
import com.jin.subwayalarm.domain.model.JourneyPlan
import com.jin.subwayalarm.domain.model.Leg
import com.jin.subwayalarm.domain.model.TripSession
import com.jin.subwayalarm.domain.model.TripState
import com.jin.subwayalarm.domain.usecase.AdvanceJourneyUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlarmViewModel @Inject constructor(
    tripSessionRepository: TripSessionRepository,
    journeyPlanRepository: JourneyPlanRepository,
    alarmSettingsRepository: AlarmSettingsRepository,
    private val stationRepository: StationRepository,
    private val advanceJourney: AdvanceJourneyUseCase,
) : ViewModel() {

    sealed interface UiState {
        /**
         * 울릴 알람이 없음 — 알림의 "확인했어요" 로 이미 껐거나, 오래된 알림을 눌러 들어온 경우.
         * [tripContinues] 면 다음 구간 추적 화면으로, 아니면 홈으로 보낸다.
         */
        data class NotRinging(val tripContinues: Boolean) : UiState

        data class Ringing(
            /** 내릴 역(환승역 또는 목적지). 알람이 울린 역이 아니다. */
            val stationName: String,
            /** 알람이 울린 역(지금 지나는 역). */
            val passedStationName: String,
            val stopsAway: Int,
            val isTransfer: Boolean,
            val nextLeg: Leg?,
            val nextLegStops: Int?,
            val nextLegNumber: Int,
            val totalLegs: Int,
            val setting: AlarmSetting,
        ) : UiState {
            /** "다음 정차역" / "2정거장 뒤" / "도착" */
            val whenLabel: String
                get() = when {
                    stopsAway <= 0 -> "도착"
                    stopsAway == 1 -> "다음 정차역"
                    else -> "${stopsAway}정거장 뒤"
                }
        }
    }

    /** null = 아직 DB 조회 전(로딩 중) — 기본값으로 환승 여부를 잘못 판단해 여정을 끝내지 않게 한다. */
    val uiState: StateFlow<UiState?> = combine(
        tripSessionRepository.observeCurrent(),
        journeyPlanRepository.observeCurrent(),
        alarmSettingsRepository.settings,
    ) { session, plan, setting -> buildUiState(session, plan, setting) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private suspend fun buildUiState(session: TripSession?, plan: JourneyPlan?, setting: AlarmSetting): UiState {
        if (session == null || session.state == TripState.DONE) return UiState.NotRinging(tripContinues = false)
        if (session.state != TripState.ALERTED) return UiState.NotRinging(tripContinues = true)

        val graph = stationRepository.stationGraph(session.lineId)
        val nextLeg = plan?.takeIf { it.hasNextLeg }?.legs?.get(plan.currentLegIndex + 1)
        return UiState.Ringing(
            stationName = graph?.station(session.destStatnId)?.name.orEmpty(),
            passedStationName = graph?.station(session.currentStatnId)?.name.orEmpty(),
            stopsAway = graph?.remainingStops(session.currentStatnId, session.destStatnId) ?: 1,
            isTransfer = nextLeg != null,
            nextLeg = nextLeg,
            nextLegStops = nextLeg?.let { legStops(it) },
            nextLegNumber = (plan?.currentLegIndex ?: 0) + 2,
            totalLegs = plan?.totalLegs ?: 1,
            setting = setting,
        )
    }

    private suspend fun legStops(leg: Leg): Int? {
        val graph = stationRepository.stationGraph(leg.lineId) ?: return null
        val from = graph.stations.firstOrNull { it.name == leg.boardStationName } ?: return null
        val to = graph.stations.firstOrNull { it.name == leg.alightStationName } ?: return null
        return graph.path(from.statnId, to.statnId)?.let { it.size - 1 }
    }

    /** 다음 구간이 있으면 이어서 추적 시작, 없으면 여정 종료. */
    fun dismiss() {
        viewModelScope.launch { advanceJourney() }
    }
}
