package com.jin.subwayalarm.ui.screen.tracking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jin.subwayalarm.data.repository.BoardingCandidateRepository
import com.jin.subwayalarm.data.repository.JourneyPlanRepository
import com.jin.subwayalarm.data.repository.StationRepository
import com.jin.subwayalarm.data.repository.TripSessionRepository
import com.jin.subwayalarm.domain.model.BoardingCandidate
import com.jin.subwayalarm.domain.model.JourneyPlan
import com.jin.subwayalarm.domain.model.TrackingMode
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
class TrackingViewModel @Inject constructor(
    tripSessionRepository: TripSessionRepository,
    journeyPlanRepository: JourneyPlanRepository,
    boardingCandidateRepository: BoardingCandidateRepository,
    private val stationRepository: StationRepository,
    private val advanceJourney: AdvanceJourneyUseCase,
) : ViewModel() {

    data class RouteStation(
        val statnId: String,
        val name: String,
        val isOrigin: Boolean,
        val isDestination: Boolean,
        val isCurrent: Boolean,
        val isPassed: Boolean,
    )

    data class UiState(
        val lineName: String = "",
        val originName: String = "",
        val destinationName: String = "",
        val currentStationName: String = "",
        val remainingStops: Int = 0,
        val mode: TrackingMode = TrackingMode.API,
        val state: TripState = TripState.MATCHING,
        val route: List<RouteStation> = emptyList(),
        val isTransferLeg: Boolean = false,
        val nextLineName: String = "",
        val currentLegNumber: Int = 1,
        val totalLegs: Int = 1,
        val isActive: Boolean = false,
        val candidates: List<BoardingCandidate> = emptyList(),
        val boardedTrainNo: String? = null,
        val showRematch: Boolean = false,
    )

    val uiState: StateFlow<UiState?> = combine(
        tripSessionRepository.observeCurrent(),
        journeyPlanRepository.observeCurrent(),
        boardingCandidateRepository.candidates,
    ) { session, plan, candidates -> buildUiState(session, plan, candidates) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun acknowledgeAlarm() {
        viewModelScope.launch { advanceJourney() }
    }

    private suspend fun buildUiState(
        session: TripSession?,
        plan: JourneyPlan?,
        candidates: List<BoardingCandidate>,
    ): UiState {
        if (session == null || session.state == TripState.DONE) return UiState(isActive = false)
        val graph = stationRepository.stationGraph(session.lineId)
            ?: return UiState(isActive = true, state = session.state, mode = session.mode)

        val path = graph.stationsBetween(session.originStatnId, session.destStatnId)
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

        val meta = stationRepository.lineMetas().firstOrNull { it.lineId == session.lineId }
        val lineName = meta?.displayName ?: meta?.lineName.orEmpty()
        val isTransferLeg = plan?.hasNextLeg == true
        val nextLineName = if (isTransferLeg) plan!!.legs[plan.currentLegIndex + 1].lineName else ""

        return UiState(
            lineName = lineName,
            originName = graph.station(session.originStatnId)?.name.orEmpty(),
            destinationName = graph.station(session.destStatnId)?.name.orEmpty(),
            currentStationName = graph.station(session.currentStatnId)?.name.orEmpty(),
            remainingStops = graph.remainingStops(session.currentStatnId, session.destStatnId),
            mode = session.mode,
            state = session.state,
            route = route,
            isTransferLeg = isTransferLeg,
            nextLineName = nextLineName,
            currentLegNumber = (plan?.currentLegIndex ?: 0) + 1,
            totalLegs = plan?.totalLegs ?: 1,
            isActive = true,
            candidates = candidates,
            boardedTrainNo = session.boardedTrainNo,
            showRematch = session.state == TripState.TRACKING || session.state == TripState.MATCHING,
        )
    }
}
