package com.jin.subwayalarm.ui.screen.tracking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jin.subwayalarm.data.repository.AlarmSettingsRepository
import com.jin.subwayalarm.data.repository.BoardingCandidateRepository
import com.jin.subwayalarm.data.repository.JourneyPlanRepository
import com.jin.subwayalarm.data.repository.StationRepository
import com.jin.subwayalarm.data.repository.TripSessionRepository
import com.jin.subwayalarm.domain.model.AlarmSetting
import com.jin.subwayalarm.domain.model.BoardingCandidate
import com.jin.subwayalarm.domain.model.JourneyPlan
import com.jin.subwayalarm.domain.model.Station
import com.jin.subwayalarm.domain.model.TrackingMode
import com.jin.subwayalarm.domain.model.TripSession
import com.jin.subwayalarm.domain.model.TripState
import com.jin.subwayalarm.domain.usecase.AdvanceJourneyUseCase
import com.jin.subwayalarm.tracking.TrackingService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs

@HiltViewModel
class TrackingViewModel @Inject constructor(
    tripSessionRepository: TripSessionRepository,
    journeyPlanRepository: JourneyPlanRepository,
    private val boardingCandidateRepository: BoardingCandidateRepository,
    alarmSettingsRepository: AlarmSettingsRepository,
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
        /** 이 역에 들어서면 알람이 울린다. */
        val isAlert: Boolean,
    )

    data class UiState(
        val lineName: String = "",
        val originName: String = "",
        val destinationName: String = "",
        val currentStationName: String = "",
        /** 알람이 울리는 역(내릴 역 N정거장 전). */
        val alertStationName: String = "",
        val remainingStops: Int = 0,
        /** 남은 구간 평균 소요시간 합(분, 올림). 역 간 시간을 모르면 null. */
        val etaMinutes: Int? = null,
        val mode: TrackingMode = TrackingMode.API,
        val state: TripState = TripState.MATCHING,
        /** 화면에 보일 역 — 직전에 지난 역 하나부터 도착역까지. */
        val route: List<RouteStation> = emptyList(),
        val isTransferLeg: Boolean = false,
        val nextLineName: String = "",
        /** 다음 구간 승차역. 이번 하차역과 다르면 걸어서 환승. */
        val nextBoardName: String = "",
        val currentLegNumber: Int = 1,
        val totalLegs: Int = 1,
        val isActive: Boolean = false,
        val candidates: List<BoardingCandidate> = emptyList(),
        val boardedTrainNo: String? = null,
        val terminusName: String? = null,
        val lastSyncAtMillis: Long? = null,
    ) {
        val isSensorOnly: Boolean get() = boardedTrainNo == TrackingService.SENSOR_ONLY_TRAIN_NO
        val isWalkingTransfer: Boolean get() = isTransferLeg && nextBoardName.isNotEmpty() && nextBoardName != destinationName
    }

    val uiState: StateFlow<UiState?> = combine(
        tripSessionRepository.observeCurrent(),
        journeyPlanRepository.observeCurrent(),
        boardingCandidateRepository.candidates,
        alarmSettingsRepository.settings,
    ) { session, plan, candidates, setting -> buildUiState(session, plan, candidates, setting) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun acknowledgeAlarm() {
        viewModelScope.launch { advanceJourney() }
    }

    private suspend fun buildUiState(
        session: TripSession?,
        plan: JourneyPlan?,
        candidates: List<BoardingCandidate>,
        setting: AlarmSetting,
    ): UiState {
        if (session == null || session.state == TripState.DONE) return UiState(isActive = false)
        val graph = stationRepository.stationGraph(session.lineId)
            ?: return UiState(isActive = true, state = session.state, mode = session.mode)

        val path = graph.stationsBetween(session.originStatnId, session.destStatnId)
        val currentIndex = path.indexOfFirst { it.statnId == session.currentStatnId }
        // EvaluateAlertUseCase 와 같은 규칙: 알람 역 = 출발→도착 경로에서 도착 N정거장 전(1..경로길이-1 로 고정).
        val alertIndex = if (path.size >= 2) path.lastIndex - setting.leadStations.coerceIn(1, path.lastIndex) else -1
        val firstShown = (currentIndex - 1).coerceAtLeast(0)
        val route = path.mapIndexed { i, st ->
            RouteStation(
                statnId = st.statnId,
                name = st.name,
                isOrigin = i == 0,
                isDestination = i == path.lastIndex,
                isCurrent = st.statnId == session.currentStatnId,
                isPassed = currentIndex >= 0 && i < currentIndex,
                isAlert = i == alertIndex,
            )
        }.drop(firstShown)

        val meta = stationRepository.lineMetas().firstOrNull { it.lineId == session.lineId }
        val lineName = meta?.displayName ?: meta?.lineName.orEmpty()
        val nextLeg = plan?.takeIf { it.hasNextLeg }?.legs?.get(plan.currentLegIndex + 1)
        val remaining = if (currentIndex >= 0) path.drop(currentIndex) else path

        return UiState(
            lineName = lineName,
            originName = graph.station(session.originStatnId)?.name.orEmpty(),
            destinationName = graph.station(session.destStatnId)?.name.orEmpty(),
            currentStationName = graph.station(session.currentStatnId)?.name.orEmpty(),
            alertStationName = path.getOrNull(alertIndex)?.name.orEmpty(),
            remainingStops = graph.remainingStops(session.currentStatnId, session.destStatnId),
            etaMinutes = etaMinutes(remaining),
            mode = session.mode,
            state = session.state,
            route = route,
            isTransferLeg = nextLeg != null,
            nextLineName = nextLeg?.lineName.orEmpty(),
            nextBoardName = nextLeg?.boardStationName.orEmpty(),
            currentLegNumber = (plan?.currentLegIndex ?: 0) + 1,
            totalLegs = plan?.totalLegs ?: 1,
            isActive = true,
            candidates = candidates,
            boardedTrainNo = session.boardedTrainNo,
            terminusName = boardingCandidateRepository.terminusOf(session.boardedTrainNo),
            lastSyncAtMillis = session.lastSyncAtEpochMillis.takeIf { it > 0 },
        )
    }

    /**
     * 남은 역 사이 평균 소요시간 합. avgSecToNext 는 순번 증가 방향 기준이라, 내려가는 구간은 다음 역의 값을 쓴다.
     * 순환선 닫힘 구간(순번 차 > 1)처럼 값이 없으면 [FALLBACK_SEC_PER_HOP].
     */
    private fun etaMinutes(remaining: List<Station>): Int? {
        if (remaining.size < 2) return null
        val seconds = remaining.zipWithNext().sumOf { (a, b) ->
            val sec = when {
                abs(a.order - b.order) != 1 -> 0
                b.order > a.order -> a.avgSecToNext
                else -> b.avgSecToNext
            }
            if (sec > 0) sec else FALLBACK_SEC_PER_HOP
        }
        return (seconds + 59) / 60
    }

    private companion object {
        const val FALLBACK_SEC_PER_HOP = 120
    }
}
