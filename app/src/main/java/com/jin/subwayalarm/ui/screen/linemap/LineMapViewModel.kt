package com.jin.subwayalarm.ui.screen.linemap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jin.subwayalarm.data.repository.PositionFetchResult
import com.jin.subwayalarm.data.repository.StationRepository
import com.jin.subwayalarm.data.repository.TrainPositionRepository
import com.jin.subwayalarm.domain.model.LineMeta
import com.jin.subwayalarm.domain.usecase.BuildLineBoardUseCase
import com.jin.subwayalarm.domain.usecase.LineBoardRow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 노선 실시간 현황판. 선택한 노선의 전체 역을 나열하고 지금 어느 역에 열차가 있는지 보여준다.
 * 화면이 떠 있는 동안만 주기 폴링(ViewModel 소멸 시 함께 종료 — 별도 서비스 없음).
 */
@HiltViewModel
class LineMapViewModel @Inject constructor(
    private val stationRepository: StationRepository,
    private val trainPositionRepository: TrainPositionRepository,
    private val buildLineBoard: BuildLineBoardUseCase,
) : ViewModel() {

    data class UiState(
        val lines: List<LineMeta> = emptyList(),
        val selectedLineId: String? = null,
        val rows: List<LineBoardRow> = emptyList(),
        /** 화면에 표시된 열차 수(시드 구간 안). */
        val shownTrainCount: Int = 0,
        /** 노선엔 있으나 시드 구간 밖이라 표시 못 한 열차 수(예: 1호선 경인선 밖). */
        val hiddenTrainCount: Int = 0,
        val loading: Boolean = false,
        val updatedAtMillis: Long? = null,
        val error: String? = null,
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var pollJob: Job? = null

    init {
        viewModelScope.launch {
            stationRepository.ensureSeeded()
            // 현황판은 실시간 지원 노선만 (미지원 노선은 폴링 무의미)
            val lines = stationRepository.lineMetas().filter { it.supportsRealtime }
            _uiState.update { it.copy(lines = lines) }
            lines.firstOrNull()?.let { selectLine(it.lineId) }
        }
    }

    fun selectLine(lineId: String) {
        if (_uiState.value.selectedLineId == lineId && pollJob?.isActive == true) return
        pollJob?.cancel()
        _uiState.update {
            it.copy(selectedLineId = lineId, rows = emptyList(), shownTrainCount = 0, hiddenTrainCount = 0, loading = true, error = null)
        }
        pollJob = viewModelScope.launch {
            val meta = stationRepository.lineMetas().firstOrNull { it.lineId == lineId } ?: return@launch
            val stations = stationRepository.stations(lineId)
            while (isActive) {
                when (val fetch = trainPositionRepository.fetchRealtimePosition(meta.lineName)) {
                    is PositionFetchResult.Success -> {
                        val rows = buildLineBoard(stations, meta.directionPolarity, fetch.positions, lineId)
                        val shown = rows.sumOf { it.trains.size }
                        val totalOnLine = fetch.positions.count { it.lineId == lineId }
                        _uiState.update {
                            it.copy(
                                rows = rows,
                                shownTrainCount = shown,
                                hiddenTrainCount = (totalOnLine - shown).coerceAtLeast(0),
                                loading = false,
                                updatedAtMillis = System.currentTimeMillis(),
                                error = null,
                            )
                        }
                    }
                    is PositionFetchResult.RateLimited ->
                        _uiState.update { it.copy(loading = false, error = "호출 한도 초과 — 잠시 후 자동 재시도") }
                    is PositionFetchResult.Error ->
                        _uiState.update { it.copy(loading = false, error = fetch.message) }
                }
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    private companion object {
        /** 현황판은 보고 있는 동안만 폴링하므로 추적 루프보다 짧게 가져가도 한도 부담이 작다. */
        const val POLL_INTERVAL_MS = 15_000L
    }
}
