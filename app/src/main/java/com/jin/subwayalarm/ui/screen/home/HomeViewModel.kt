package com.jin.subwayalarm.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jin.subwayalarm.data.repository.FavoriteRouteRepository
import com.jin.subwayalarm.data.repository.JourneyPlanRepository
import com.jin.subwayalarm.data.repository.StationRepository
import com.jin.subwayalarm.data.repository.TripSessionRepository
import com.jin.subwayalarm.domain.model.FavoriteRoute
import com.jin.subwayalarm.domain.model.JourneyPlan
import com.jin.subwayalarm.domain.model.Leg
import com.jin.subwayalarm.domain.model.LineMeta
import com.jin.subwayalarm.domain.model.Station
import com.jin.subwayalarm.domain.model.SubwayNetwork
import com.jin.subwayalarm.domain.model.TripSession
import com.jin.subwayalarm.domain.model.TripState
import com.jin.subwayalarm.domain.usecase.JourneyPlannerUseCase
import com.jin.subwayalarm.domain.usecase.NearestStationUseCase
import com.jin.subwayalarm.location.LocationProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val stationRepository: StationRepository,
    private val tripSessionRepository: TripSessionRepository,
    private val journeyPlanRepository: JourneyPlanRepository,
    private val favoriteRouteRepository: FavoriteRouteRepository,
    private val journeyPlanner: JourneyPlannerUseCase,
    private val locationProvider: LocationProvider,
    private val nearestStation: NearestStationUseCase,
) : ViewModel() {

    /** 검색 목록에 보여줄 역 하나. 같은 역명을 지나는 노선이 여럿이면 배지로 전부 표시. */
    data class StationOption(val name: String, val lineNames: List<String>)

    /** 계산된 구간 하나 + 그 구간의 정거장 수(그래프에서 못 찾으면 null). */
    data class LegInfo(val leg: Leg, val stops: Int?)

    /** 즐겨찾기 카드 요약 — "약수 환승 · 20정거장". legs 가 비면 경로를 못 찾은 것. */
    data class RouteSummary(val legs: List<LegInfo>) {
        val found: Boolean get() = legs.isNotEmpty()
        val lineNames: List<String> get() = legs.map { it.leg.lineName }
        val totalStops: Int? get() = if (legs.any { it.stops == null }) null else legs.sumOf { it.stops ?: 0 }

        /** "환승 없음" / "약수 환승" / "이수 걸어서 환승" / "약수·종로3가 환승" */
        val transferText: String
            get() {
                if (legs.size <= 1) return "환승 없음"
                val transfers = legs.zipWithNext()
                val names = transfers.joinToString("·") { (a, _) -> a.leg.alightStationName }
                val walking = transfers.any { (a, b) -> a.leg.alightStationName != b.leg.boardStationName }
                return if (walking) "$names 걸어서 환승" else "$names 환승"
            }
    }

    data class UiState(
        val allStations: List<StationOption> = emptyList(),
        val originName: String? = null,
        val destinationName: String? = null,
        val detecting: Boolean = false,
        val detectMessage: String? = null,
        /** 계산된 경로 미리보기. 비어있으면 미계산이거나 경로 없음. */
        val legs: List<LegInfo> = emptyList(),
        val planning: Boolean = false,
        val routeError: String? = null,
        /** 경로에 실시간 미지원 구간이 있으면 안내 문구. */
        val realtimeWarning: String? = null,
        /** 저장한 즐겨찾기 경로 (최근 사용 순). */
        val favorites: List<FavoriteRoute> = emptyList(),
        /** 즐겨찾기별 요약. 아직 계산 전이면 키가 없다. */
        val favoriteSummaries: Map<FavoriteRoute, RouteSummary> = emptyMap(),
        val editingFavorites: Boolean = false,
        /** 즐겨찾기 "출발 →" 을 눌러 시작 준비 중인 경로(버튼 중복 탭 방지). */
        val startingFavorite: FavoriteRoute? = null,
        /** 진행 중인 여정 "가락시장 → 동묘앞". 없으면 null. */
        val activeTripLabel: String? = null,
    ) {
        val canStart: Boolean get() = legs.isNotEmpty() && !planning

        /** 지금 고른 출발/도착이 즐겨찾기에 들어 있는지 — 저장 버튼 상태. */
        val currentRoute: FavoriteRoute?
            get() = if (originName != null && destinationName != null) {
                FavoriteRoute(originName, destinationName)
            } else {
                null
            }

        val isCurrentRouteFavorite: Boolean get() = currentRoute?.let { it in favorites } == true

        fun linesOf(stationName: String?): List<String> =
            allStations.firstOrNull { it.name == stationName }?.lineNames.orEmpty()
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val networkReady = CompletableDeferred<SubwayNetwork>()
    private var allStationsFlat: List<Station> = emptyList()
    private var lineMetaById: Map<String, LineMeta> = emptyMap()

    /** 진행 중인 경로 계산 — 선택이 바뀌면 취소해서 늦게 끝난 이전 계산이 최신 결과를 덮지 않게 한다. */
    private var planJob: Job? = null
    private var summaryJob: Job? = null

    init {
        viewModelScope.launch {
            stationRepository.ensureSeeded()
            val net = stationRepository.buildNetwork()
            val metas = stationRepository.lineMetas()
            lineMetaById = metas.associateBy { it.lineId }
            allStationsFlat = metas.flatMap { stationRepository.stations(it.lineId) }

            val options = allStationsFlat
                .groupBy { it.name }
                .map { (name, stations) -> StationOption(name, stations.map { net.displayName(it.lineId) }.distinct()) }
                .sortedBy { it.name }
            _uiState.update { it.copy(allStations = options) }
            networkReady.complete(net)
        }
        viewModelScope.launch {
            favoriteRouteRepository.observeAll().collect { saved ->
                _uiState.update {
                    it.copy(favorites = saved, editingFavorites = it.editingFavorites && saved.isNotEmpty())
                }
                refreshFavoriteSummaries(saved)
            }
        }
        viewModelScope.launch {
            combine(tripSessionRepository.observeCurrent(), journeyPlanRepository.observeCurrent()) { session, plan ->
                activeLabel(session, plan)
            }.collect { label -> _uiState.update { it.copy(activeTripLabel = label) } }
        }
    }

    private fun activeLabel(session: TripSession?, plan: JourneyPlan?): String? {
        if (session == null || session.state == TripState.DONE) return null
        val legs = plan?.legs.orEmpty()
        return if (legs.isEmpty()) "진행 중인 여정" else "${legs.first().boardStationName} → ${legs.last().alightStationName}"
    }

    private fun refreshFavoriteSummaries(saved: List<FavoriteRoute>) {
        summaryJob?.cancel()
        summaryJob = viewModelScope.launch {
            val net = networkReady.await()
            val known = _uiState.value.favoriteSummaries
            val summaries = saved.associateWith { route ->
                known[route] ?: RouteSummary(plan(net, route.originName, route.destinationName).orEmpty())
            }
            _uiState.update { it.copy(favoriteSummaries = summaries) }
        }
    }

    private suspend fun plan(net: SubwayNetwork, origin: String, dest: String): List<LegInfo>? {
        // 전체 노선망 탐색이라 메인 스레드에서 돌리면 화면이 멈춘다.
        val legs = withContext(Dispatchers.Default) { journeyPlanner(net, origin, dest) } ?: return null
        return legs.map { LegInfo(it, legStops(it)) }
    }

    private suspend fun legStops(leg: Leg): Int? {
        val graph = stationRepository.stationGraph(leg.lineId) ?: return null
        val from = graph.stations.firstOrNull { it.name == leg.boardStationName } ?: return null
        val to = graph.stations.firstOrNull { it.name == leg.alightStationName } ?: return null
        return graph.path(from.statnId, to.statnId)?.let { it.size - 1 }
    }

    /** 저장 버튼 — 지금 고른 경로를 즐겨찾기에 넣거나 뺀다. */
    fun toggleFavorite() {
        val route = _uiState.value.currentRoute ?: return
        val alreadySaved = _uiState.value.isCurrentRouteFavorite
        viewModelScope.launch {
            if (alreadySaved) favoriteRouteRepository.remove(route) else favoriteRouteRepository.add(route)
        }
    }

    /** 즐겨찾기 카드 본문을 눌러 출발·도착을 채우고 경로를 다시 계산한다(바로 출발하지 않음). */
    fun applyFavorite(route: FavoriteRoute) {
        _uiState.update {
            it.copy(originName = route.originName, destinationName = route.destinationName, detectMessage = null)
        }
        recomputeRoute()
    }

    fun removeFavorite(route: FavoriteRoute) {
        viewModelScope.launch { favoriteRouteRepository.remove(route) }
    }

    fun toggleEditFavorites() {
        _uiState.update { it.copy(editingFavorites = !it.editingFavorites) }
    }

    fun selectOrigin(name: String) {
        _uiState.update { it.copy(originName = name, detectMessage = null) }
        recomputeRoute()
    }

    fun selectDestination(name: String) {
        _uiState.update { it.copy(destinationName = name) }
        recomputeRoute()
    }

    /** ↻ 바꾸기 — 출발과 도착을 맞바꾼다(퇴근길). */
    fun swapStations() {
        _uiState.update {
            it.copy(originName = it.destinationName, destinationName = it.originName, detectMessage = null)
        }
        recomputeRoute()
    }

    private fun recomputeRoute() {
        val state = _uiState.value
        val origin = state.originName
        val dest = state.destinationName
        planJob?.cancel()
        if (origin == null || dest == null) {
            _uiState.update { it.copy(legs = emptyList(), routeError = null, realtimeWarning = null, planning = false) }
            return
        }
        planJob = viewModelScope.launch {
            _uiState.update { it.copy(planning = true, routeError = null) }
            val legs = plan(networkReady.await(), origin, dest)
            _uiState.update {
                if (legs == null) {
                    it.copy(planning = false, legs = emptyList(), routeError = "경로를 찾을 수 없어요", realtimeWarning = null)
                } else {
                    it.copy(planning = false, legs = legs, routeError = null, realtimeWarning = realtimeWarning(legs))
                }
            }
        }
    }

    private fun realtimeWarning(legs: List<LegInfo>): String? {
        val noRt = legs.mapNotNull { info ->
            lineMetaById[info.leg.lineId]?.takeIf { !it.supportsRealtime }?.displayName
        }.distinct()
        return if (noRt.isEmpty()) null else "일부 구간(${noRt.joinToString("·")})은 실시간 추적이 안 돼요. 경로·환승 안내용입니다."
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
                        detectMessage = "가장 가까운 역 · 약 ${nearest.distanceMeters.toInt()}m",
                    )
                }
                recomputeRoute()
            }
        }
    }

    /**
     * "새 경로" 의 추적 시작. 여정을 먼저 저장한다(레이스 컨디션 방지 — TrackingService 는 이 값을 그대로 이어받는다).
     * @return 저장 성공 여부. false 면 호출측은 서비스 시작·화면 전환을 하면 안 된다.
     */
    suspend fun startTrip(): Boolean {
        val state = _uiState.value
        if (!state.canStart) return false
        val ok = saveTrip(state.legs.map { it.leg })
        if (ok) state.currentRoute?.takeIf { it in state.favorites }?.let { favoriteRouteRepository.markUsed(it) }
        return ok
    }

    /** 즐겨찾기 카드의 "출발 →" — 경로를 (필요하면) 다시 계산하고 바로 여정을 저장한다. */
    suspend fun startFavorite(route: FavoriteRoute): Boolean {
        if (_uiState.value.startingFavorite != null) return false
        _uiState.update { it.copy(startingFavorite = route) }
        try {
            val cached = _uiState.value.favoriteSummaries[route]?.legs.orEmpty()
            val legs = cached.ifEmpty {
                plan(networkReady.await(), route.originName, route.destinationName).orEmpty()
            }
            if (legs.isEmpty()) {
                _uiState.update { it.copy(routeError = "${route.originName} → ${route.destinationName} 경로를 찾을 수 없어요") }
                return false
            }
            val ok = saveTrip(legs.map { it.leg })
            if (ok) favoriteRouteRepository.markUsed(route)
            return ok
        } finally {
            _uiState.update { it.copy(startingFavorite = null) }
        }
    }

    private suspend fun saveTrip(legs: List<Leg>): Boolean {
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
