package com.jin.subwayalarm.ui.navigation

import androidx.lifecycle.ViewModel
import com.jin.subwayalarm.data.repository.TripSessionRepository
import com.jin.subwayalarm.domain.model.TripState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** 앱 전체 흐름용 — 앱을 열었을 때 진행 중인 여정이 있으면 바로 추적 화면으로 보내기 위해. */
@HiltViewModel
class AppViewModel @Inject constructor(
    private val tripSessionRepository: TripSessionRepository,
) : ViewModel() {

    suspend fun hasActiveTrip(): Boolean {
        val session = tripSessionRepository.current() ?: return false
        return session.state != TripState.DONE
    }
}
