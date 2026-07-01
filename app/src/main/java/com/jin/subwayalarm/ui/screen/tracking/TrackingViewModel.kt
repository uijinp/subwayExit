package com.jin.subwayalarm.ui.screen.tracking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jin.subwayalarm.data.repository.StationRepository
import com.jin.subwayalarm.data.repository.TripSessionRepository
import com.jin.subwayalarm.domain.model.TrackingMode
import com.jin.subwayalarm.domain.model.TripSession
import com.jin.subwayalarm.domain.model.TripState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class TrackingViewModel @Inject constructor(
    tripSessionRepository: TripSessionRepository,
    private val stationRepository: StationRepository,
) : ViewModel() {

    data class UiState(
        val destinationName: String = "",
        val currentStationName: String = "",
        val remainingStops: Int = 0,
        val mode: TrackingMode = TrackingMode.API,
        val state: TripState = TripState.MATCHING,
        /** 세션이 없으면(정지·완료 등) 홈으로 돌아가야 함. */
        val isActive: Boolean = false,
    )

    val uiState: StateFlow<UiState> = tripSessionRepository.observeCurrent()
        .map { buildUiState(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    private suspend fun buildUiState(session: TripSession?): UiState {
        if (session == null || session.state == TripState.DONE) return UiState(isActive = false)
        val graph = stationRepository.stationGraph(session.lineId)
        return UiState(
            destinationName = graph?.station(session.destStatnId)?.name.orEmpty(),
            currentStationName = graph?.station(session.currentStatnId)?.name.orEmpty(),
            remainingStops = graph?.remainingStops(session.currentStatnId, session.destStatnId) ?: 0,
            mode = session.mode,
            state = session.state,
            isActive = true,
        )
    }
}
