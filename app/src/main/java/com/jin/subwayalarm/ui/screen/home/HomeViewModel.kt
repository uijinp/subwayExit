package com.jin.subwayalarm.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jin.subwayalarm.data.repository.JourneyPlanRepository
import com.jin.subwayalarm.data.repository.StationRepository
import com.jin.subwayalarm.data.repository.TripSessionRepository
import com.jin.subwayalarm.domain.model.JourneyPlan
import com.jin.subwayalarm.domain.model.Leg
import com.jin.subwayalarm.domain.model.Station
import com.jin.subwayalarm.domain.model.SubwayNetwork
import com.jin.subwayalarm.domain.model.TripSession
import com.jin.subwayalarm.domain.usecase.JourneyPlannerUseCase
import com.jin.subwayalarm.domain.usecase.NearestStationUseCase
import com.jin.subwayalarm.location.LocationProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val stationRepository: StationRepository,
    private val tripSessionRepository: TripSessionRepository,
    private val journeyPlanRepository: JourneyPlanRepository,
    private val journeyPlanner: JourneyPlannerUseCase,
    private val locationProvider: LocationProvider,
    private val nearestStation: NearestStationUseCase,
) : ViewModel() {

    /** 검색 목록에 보여줄 역 하나. 같은 역명을 지나는 노선이 여럿이면 배지로 전부 표시. */
    data class StationOption(val name: String, val lineNames: List<String>)

    data class UiState(
        val allStations: List<StationOption> = emptyList(),
        val originName: String? = null,
        val destinationName: String? = null,
        val detecting: Boolean = false,
        val detectMessage: String? = null,
        /** 계산된 경로 미리보기. 비어있으면 미계산이거나 경로 없음. */
        val legs: List<Leg> = emptyList(),
        val planning: Boolean = false,
        val routeError: String? = null,
        /** 경로에 실시간 미지원 구간이 있으면 안내 문구. */
        val realtimeWarning: String? = null,
    ) {
        val canStart: Boolean get() = legs.isNotEmpty()
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var network: SubwayNetwork? = null
    private var allStationsFlat: List<Station> = emptyList()
    private var lineMetaById: Map<String, com.jin.subwayalarm.domain.model.LineMeta> = emptyMap()

    /** 진행 중인 경로 계산 — 선택이 바뀌면 취소해서 늦게 끝난 이전 계산이 최신 결과를 덮지 않게 한다. */
    private var planJob: Job? = null

    init {
        viewModelScope.launch {
            stationRepository.ensureSeeded()
            val net = stationRepository.buildNetwork()
            network = net
            val metas = stationRepository.lineMetas()
            lineMetaById = metas.associateBy { it.lineId }
            allStationsFlat = metas.flatMap { stationRepository.stations(it.lineId) }

            val options = allStationsFlat
                .groupBy { it.name }
                .map { (name, stations) -> StationOption(name, stations.map { net.displayName(it.lineId) }.distinct()) }
                .sortedBy { it.name }
            _uiState.update { it.copy(allStations = options) }
        }
    }

    fun selectOrigin(name: String) {
        _uiState.update { it.copy(originName = name, detectMessage = null) }
        recomputeRoute()
    }

    fun selectDestination(name: String) {
        _uiState.update { it.copy(destinationName = name) }
        recomputeRoute()
    }

    private fun recomputeRoute() {
        val state = _uiState.value
        val origin = state.originName
        val dest = state.destinationName
        val net = network
        if (origin == null || dest == null || net == null) {
            planJob?.cancel()
            _uiState.update { it.copy(legs = emptyList(), routeError = null) }
            return
        }
        planJob?.cancel()
        planJob = viewModelScope.launch {
            _uiState.update { it.copy(planning = true, routeError = null) }
            val legs = journeyPlanner(net, origin, dest)
            _uiState.update {
                if (legs == null) {
                    it.copy(planning = false, legs = emptyList(), routeError = "경로를 찾을 수 없어요", realtimeWarning = null)
                } else {
                    val noRt = legs.mapNotNull { leg ->
                        lineMetaById[leg.lineId]?.takeIf { !it.supportsRealtime }?.displayName
                    }.distinct()
                    val warn = if (noRt.isEmpty()) null else
                        "일부 구간(${noRt.joinToString("·")})은 실시간 추적이 안 돼요. 경로·환승 안내용입니다."
                    it.copy(planning = false, legs = legs, routeError = null, realtimeWarning = warn)
                }
            }
        }
    }

    /**
     * 현재 위치(Wi-Fi/기지국)로 가장 가까운 역을 찾아 출발역으로 채운다.
     * 위치 권한은 호출 전 UI 에서 확보되어 있어야 한다(없으면 currentLatLon()이 null 반환).
     */
    fun detectOrigin() {
        viewModelScope.launch {
            _uiState.update { it.copy(detecting = true, detectMessage = null) }
            val here = locationProvider.currentLatLon()
            if (here == null) {
                _uiState.update { it.copy(detecting = false, detectMessage = "위치를 가져오지 못했어요") }
                return@launch
            }
            val nearest = nearestStation(here, allStationsFlat)
            if (nearest == null) {
                _uiState.update { it.copy(detecting = false, detectMessage = "근처에서 역을 찾지 못했어요") }
            } else {
                _uiState.update {
                    it.copy(
                        detecting = false,
                        originName = nearest.station.name,
                        detectMessage = "${nearest.station.name} (약 ${nearest.distanceMeters.toInt()}m)",
                    )
                }
                recomputeRoute()
            }
        }
    }

    /**
     * 추적 시작 전 여정을 먼저 저장(레이스 컨디션 방지 — TrackingService 는 이 값을 그대로 이어받는다).
     * @return 저장 성공 여부. false 면 호출측은 서비스 시작·화면 전환을 하면 안 된다.
     */
    suspend fun startTrip(): Boolean {
        val legs = _uiState.value.legs
        if (legs.isEmpty()) return false
        val firstLeg = legs.first()
        val stations = stationRepository.stations(firstLeg.lineId)
        val originId = stations.firstOrNull { it.name == firstLeg.boardStationName }?.statnId
        val destId = stations.firstOrNull { it.name == firstLeg.alightStationName }?.statnId
        if (originId == null || destId == null) {
            _uiState.update { it.copy(legs = emptyList(), routeError = "경로 정보가 맞지 않아요 — 역을 다시 선택해주세요") }
            return false
        }

        journeyPlanRepository.save(JourneyPlan(legs = legs, currentLegIndex = 0))
        tripSessionRepository.save(
            TripSession(
                originStatnId = originId,
                destStatnId = destId,
                lineId = firstLeg.lineId,
                currentStatnId = originId,
                lastSyncAtEpochMillis = System.currentTimeMillis(),
            ),
        )
        return true
    }
}
