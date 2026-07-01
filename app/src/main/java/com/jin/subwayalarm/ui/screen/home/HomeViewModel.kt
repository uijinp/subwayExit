package com.jin.subwayalarm.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jin.subwayalarm.data.repository.StationRepository
import com.jin.subwayalarm.data.repository.TripSessionRepository
import com.jin.subwayalarm.domain.model.Station
import com.jin.subwayalarm.domain.model.TripSession
import dagger.hilt.android.lifecycle.HiltViewModel
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
) : ViewModel() {

    data class UiState(
        val lineId: String = "",
        val lineName: String = "",
        val stations: List<Station> = emptyList(),
        val originStatnId: String? = null,
        val destStatnId: String? = null,
    ) {
        val canStart: Boolean
            get() = originStatnId != null && destStatnId != null && originStatnId != destStatnId
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            stationRepository.ensureSeeded()
            val line = stationRepository.lineMetas().firstOrNull() ?: return@launch
            val stations = stationRepository.stations(line.lineId)
            _uiState.update { it.copy(lineId = line.lineId, lineName = line.lineName, stations = stations) }
        }
    }

    fun selectOrigin(statnId: String) = _uiState.update { it.copy(originStatnId = statnId) }

    fun selectDestination(statnId: String) = _uiState.update { it.copy(destStatnId = statnId) }

    /** 추적 시작 전 여정을 먼저 저장(레이스 컨디션 방지 — TrackingService 는 이 값을 그대로 이어받는다). */
    suspend fun startTrip() {
        val state = _uiState.value
        val originId = state.originStatnId ?: return
        val destId = state.destStatnId ?: return
        tripSessionRepository.save(
            TripSession(
                originStatnId = originId,
                destStatnId = destId,
                lineId = state.lineId,
                currentStatnId = originId,
                lastSyncAtEpochMillis = System.currentTimeMillis(),
            ),
        )
    }
}
