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

    /** 여정 경로의 한 역. isCurrent = 현재 열차 위치. */
    data class RouteStation(
        val statnId: String,
        val name: String,
        val isOrigin: Boolean,
        val isDestination: Boolean,
        val isCurrent: Boolean,
        /** 현재 열차가 이미 지나온(또는 현재) 역인지 — 진행바 색칠용. */
        val isPassed: Boolean,
    )

    data class UiState(
        val destinationName: String = "",
        val currentStationName: String = "",
        val remainingStops: Int = 0,
        val mode: TrackingMode = TrackingMode.API,
        val state: TripState = TripState.MATCHING,
        val route: List<RouteStation> = emptyList(),
        /** 세션이 없으면(정지·완료 등) 홈으로 돌아가야 함. */
        val isActive: Boolean = false,
    )

    val uiState: StateFlow<UiState> = tripSessionRepository.observeCurrent()
        .map { buildUiState(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    private suspend fun buildUiState(session: TripSession?): UiState {
        if (session == null || session.state == TripState.DONE) return UiState(isActive = false)
        val graph = stationRepository.stationGraph(session.lineId)
            ?: return UiState(isActive = true, state = session.state, mode = session.mode)

        val path = graph.stationsBetween(session.originStatnId, session.destStatnId)
        // 진행 방향상 현재역의 위치(경로 내 인덱스). 없으면 -1(출발 전/이탈).
        val currentIndex = path.indexOfFirst { it.statnId == session.currentStatnId }
        val route = path.mapIndexed { i, st ->
            RouteStation(
                statnId = st.statnId,
                name = st.name,
                isOrigin = i == 0,
                isDestination = i == path.lastIndex,
                isCurrent = st.statnId == session.currentStatnId,
                isPassed = currentIndex >= 0 && i <= currentIndex,
            )
        }

        return UiState(
            destinationName = graph.station(session.destStatnId)?.name.orEmpty(),
            currentStationName = graph.station(session.currentStatnId)?.name.orEmpty(),
            remainingStops = graph.remainingStops(session.currentStatnId, session.destStatnId),
            mode = session.mode,
            state = session.state,
            route = route,
            isActive = true,
        )
    }
}
