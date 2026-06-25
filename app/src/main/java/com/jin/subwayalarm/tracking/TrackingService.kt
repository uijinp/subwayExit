package com.jin.subwayalarm.tracking

import android.content.Intent
import androidx.lifecycle.LifecycleService
import dagger.hilt.android.AndroidEntryPoint

/**
 * 추적 Foreground Service (FGS type = dataSync).
 *
 * 작업순서 7단계에서 본격 구현 예정:
 *  - startForeground() + 상시 알림(현재 추정역·정지 버튼)
 *  - 적응형 폴링 추적 루프(RealtimeTracker)
 *  - 프로세스 사망 복구(TripSession 영속화, START_REDELIVER_INTENT)
 *
 * 현재는 매니페스트 선언과 골격 컴파일을 위한 스텁.
 */
@AndroidEntryPoint
class TrackingService : LifecycleService() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        // TODO(step7): startForeground + 추적 루프 시작. 사망 복구 위해 REDELIVER.
        return START_REDELIVER_INTENT
    }
}
