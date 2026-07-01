package com.jin.subwayalarm.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jin.subwayalarm.data.repository.StationRepository
import com.jin.subwayalarm.data.repository.TripSessionRepository
import com.jin.subwayalarm.domain.model.Station
import com.jin.subwayalarm.domain.model.TripSession
import com.jin.subwayalarm.domain.usecase.NearestStationUseCase
import com.jin.subwayalarm.location.LocationProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val stationRepository: StationRepository,
    private val tripSessionRepository: TripSessionRepository,
    private val locationProvider: LocationProvider,
    private val nearestStation: NearestStationUseCase,
) : ViewModel() {

    data class LineOption(val lineId: String, val lineName: String)

    data class UiState(
        val lines: List<LineOption> = emptyList(),
        val selectedLineId: String? = null,
        val stations: List<Station> = emptyList(),
        val originStatnId: String? = null,
        val destStatnId: String? = null,
        val detecting: Boolean = false,
        val detectMessage: String? = null,
    ) {
        val selectedLineName: String
            get() = lines.firstOrNull { it.lineId == selectedLineId }?.lineName.orEmpty()

        val canStart: Boolean
            get() = selectedLineId != null && originStatnId != null && destStatnId != null &&
                originStatnId != destStatnId

        /** 출발→목적지 남은 정거장 수(선택 완료 시). stations 는 order 정렬이라 인덱스 차 = 순번 차. */
        val remainingStops: Int?
            get() {
                val o = stations.indexOfFirst { it.statnId == originStatnId }
                val d = stations.indexOfFirst { it.statnId == destStatnId }
                return if (o >= 0 && d >= 0 && o != d) abs(d - o) else null
            }
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            stationRepository.ensureSeeded()
            val lines = stationRepository.lineMetas()
                .sortedBy { it.lineId }
                .map { LineOption(it.lineId, it.lineName) }
            _uiState.update { it.copy(lines = lines) }
            lines.firstOrNull()?.let { selectLine(it.lineId) }
        }
    }

    fun selectLine(lineId: String) {
        viewModelScope.launch {
            val stations = stationRepository.stations(lineId)
            _uiState.update {
                it.copy(
                    selectedLineId = lineId,
                    stations = stations,
                    originStatnId = null,
                    destStatnId = null,
                    detectMessage = null,
                )
            }
        }
    }

    fun selectOrigin(statnId: String) = _uiState.update { it.copy(originStatnId = statnId, detectMessage = null) }

    fun selectDestination(statnId: String) = _uiState.update { it.copy(destStatnId = statnId) }

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
            val nearest = nearestStation(here, _uiState.value.stations)
            if (nearest == null) {
                _uiState.update {
                    it.copy(detecting = false, detectMessage = "근처에서 ${it.selectedLineName} 역을 찾지 못했어요")
                }
            } else {
                _uiState.update {
                    it.copy(
                        detecting = false,
                        originStatnId = nearest.station.statnId,
                        detectMessage = "${nearest.station.name} (약 ${nearest.distanceMeters.toInt()}m)",
                    )
                }
            }
        }
    }

    /** 추적 시작 전 여정을 먼저 저장(레이스 컨디션 방지 — TrackingService 는 이 값을 그대로 이어받는다). */
    suspend fun startTrip() {
        val state = _uiState.value
        val lineId = state.selectedLineId ?: return
        val originId = state.originStatnId ?: return
        val destId = state.destStatnId ?: return
        tripSessionRepository.save(
            TripSession(
                originStatnId = originId,
                destStatnId = destId,
                lineId = lineId,
                currentStatnId = originId,
                lastSyncAtEpochMillis = System.currentTimeMillis(),
            ),
        )
    }
}
