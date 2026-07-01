package com.jin.subwayalarm.ui.screen.alarm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jin.subwayalarm.data.repository.JourneyPlanRepository
import com.jin.subwayalarm.data.repository.StationRepository
import com.jin.subwayalarm.data.repository.TripSessionRepository
import com.jin.subwayalarm.domain.model.JourneyPlan
import com.jin.subwayalarm.domain.model.TripSession
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
    private val stationRepository: StationRepository,
    private val advanceJourney: AdvanceJourneyUseCase,
) : ViewModel() {

    data class UiState(
        val stationName: String = "",
        val destinationName: String = "",
        val isTransfer: Boolean = false,
        val nextLineName: String = "",
    )

    /** null = 아직 DB 조회 전(로딩 중) — 기본값 isTransfer=false 로 환승 여부를 잘못 판단하지 않게 한다. */
    val uiState: StateFlow<UiState?> = combine(
        tripSessionRepository.observeCurrent(),
        journeyPlanRepository.observeCurrent(),
    ) { session, plan -> buildUiState(session, plan) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private suspend fun buildUiState(session: TripSession?, plan: JourneyPlan?): UiState {
        if (session == null) return UiState()
        val graph = stationRepository.stationGraph(session.lineId)
        val isTransfer = plan?.hasNextLeg == true
        return UiState(
            stationName = graph?.station(session.currentStatnId)?.name.orEmpty(),
            destinationName = graph?.station(session.destStatnId)?.name.orEmpty(),
            isTransfer = isTransfer,
            nextLineName = if (isTransfer) plan!!.legs[plan.currentLegIndex + 1].lineName else "",
        )
    }

    /** 다음 구간이 있으면 이어서 추적 시작, 없으면 여정 종료. */
    fun dismiss() {
        viewModelScope.launch { advanceJourney() }
    }
}
