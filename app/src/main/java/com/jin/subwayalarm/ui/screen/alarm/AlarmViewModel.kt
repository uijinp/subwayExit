package com.jin.subwayalarm.ui.screen.alarm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jin.subwayalarm.alarm.AlarmController
import com.jin.subwayalarm.data.repository.StationRepository
import com.jin.subwayalarm.data.repository.TripSessionRepository
import com.jin.subwayalarm.domain.model.TripSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlarmViewModel @Inject constructor(
    private val alarmController: AlarmController,
    private val tripSessionRepository: TripSessionRepository,
    private val stationRepository: StationRepository,
) : ViewModel() {

    data class UiState(val stationName: String = "", val destinationName: String = "")

    val uiState: StateFlow<UiState> = tripSessionRepository.observeCurrent()
        .map { buildUiState(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    private suspend fun buildUiState(session: TripSession?): UiState {
        if (session == null) return UiState()
        val graph = stationRepository.stationGraph(session.lineId)
        return UiState(
            stationName = graph?.station(session.currentStatnId)?.name.orEmpty(),
            destinationName = graph?.station(session.destStatnId)?.name.orEmpty(),
        )
    }

    fun dismiss() {
        alarmController.dismiss()
        viewModelScope.launch { tripSessionRepository.clear() }
    }
}
