package com.jin.subwayalarm.data.repository

import com.jin.subwayalarm.domain.model.BoardingCandidate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 마지막 폴링의 탑승 후보 (Room 비저장 — 사망 복구 시 다음 폴링으로 재계산).
 */
@Singleton
class BoardingCandidateRepository @Inject constructor() {
    private val _candidates = MutableStateFlow<List<BoardingCandidate>>(emptyList())
    val candidates: StateFlow<List<BoardingCandidate>> = _candidates.asStateFlow()

    fun publish(list: List<BoardingCandidate>) {
        _candidates.value = list
    }

    fun clear() {
        _candidates.value = emptyList()
    }
}
