package com.jin.subwayalarm.domain.usecase

import com.jin.subwayalarm.domain.model.TrackingMode
import com.jin.subwayalarm.domain.model.TripSession
import com.jin.subwayalarm.domain.model.TripState
import javax.inject.Inject

/**
 * 사용자 수동 탑승 확정/재매칭 — boardedTrainNo 교체 후 TRACKING.
 */
class ConfirmBoardedTrainUseCase @Inject constructor() {
    operator fun invoke(
        session: TripSession,
        trainNo: String,
        nowEpochMillis: Long,
        currentStatnId: String? = null,
    ): TripSession = session.copy(
        boardedTrainNo = trainNo,
        state = TripState.TRACKING,
        mode = TrackingMode.API,
        currentStatnId = currentStatnId ?: session.currentStatnId,
        lastSyncAtEpochMillis = nowEpochMillis,
    )
}
