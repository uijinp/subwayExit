package com.jin.subwayalarm.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jin.subwayalarm.data.repository.AlarmSettingsRepository
import com.jin.subwayalarm.data.repository.StationRepository
import com.jin.subwayalarm.data.repository.TripSessionRepository
import com.jin.subwayalarm.domain.model.AlarmSetting
import com.jin.subwayalarm.domain.model.TripSession
import com.jin.subwayalarm.domain.model.TripState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val alarmSettingsRepository: AlarmSettingsRepository,
    tripSessionRepository: TripSessionRepository,
    private val stationRepository: StationRepository,
) : ViewModel() {

    data class UiState(
        val setting: AlarmSetting = AlarmSetting(),
        /** 진행 중인 여정이 있으면 알람 시점별로 울릴 역 이름(1..3 → 역명). 없으면 빈 맵. */
        val previewStations: Map<Int, String> = emptyMap(),
    ) {
        val previewStation: String? get() = previewStations[setting.leadStations]
    }

    val uiState: StateFlow<UiState> = combine(
        alarmSettingsRepository.settings,
        tripSessionRepository.observeCurrent(),
    ) { setting, session -> UiState(setting, previewStations(session)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState(alarmSettingsRepository.current()))

    private suspend fun previewStations(session: TripSession?): Map<Int, String> {
        if (session == null || session.state == TripState.DONE) return emptyMap()
        val graph = stationRepository.stationGraph(session.lineId) ?: return emptyMap()
        val path = graph.stationsBetween(session.originStatnId, session.destStatnId)
        if (path.size < 2) return emptyMap()
        // EvaluateAlertUseCase 와 같은 규칙(경로 길이를 넘으면 출발역으로 고정).
        return AlarmSetting.LEAD_STATION_CHOICES.associateWith { lead ->
            path[path.lastIndex - lead.coerceIn(1, path.lastIndex)].name
        }
    }

    fun setLeadStations(lead: Int) = alarmSettingsRepository.update {
        it.copy(leadStations = lead.coerceIn(AlarmSetting.LEAD_STATION_CHOICES))
    }

    fun setSoundEnabled(enabled: Boolean) = alarmSettingsRepository.update { it.copy(soundEnabled = enabled) }

    fun setRepeatUntilDismissed(enabled: Boolean) = alarmSettingsRepository.update { it.copy(repeatUntilDismissed = enabled) }

    fun setShowOnLockScreen(enabled: Boolean) = alarmSettingsRepository.update { it.copy(showOnLockScreen = enabled) }
}
