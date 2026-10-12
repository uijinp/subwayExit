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

    /**
     * 후보로 본 열차의 행선지. 탑승 확정 후엔 후보 목록이 비워지므로 추적 화면의 "구파발행" 표시는 여기서 찾는다.
     * 프로세스가 죽으면 사라진다(다음 매칭 때 다시 채워짐) — 표시용이라 괜찮다.
     */
    private val terminusByTrainNo = java.util.concurrent.ConcurrentHashMap<String, String>()

    fun publish(list: List<BoardingCandidate>) {
        if (terminusByTrainNo.size > MAX_REMEMBERED) terminusByTrainNo.clear()
        list.forEach { c -> c.terminusName?.let { terminusByTrainNo[c.trainNo] = it } }
        _candidates.value = list
    }

    fun terminusOf(trainNo: String?): String? = trainNo?.let { terminusByTrainNo[it] }

    fun clear() {
        _candidates.value = emptyList()
    }

    private companion object {
        const val MAX_REMEMBERED = 200
    }
}
