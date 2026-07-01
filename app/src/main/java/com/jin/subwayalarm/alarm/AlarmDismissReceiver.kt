package com.jin.subwayalarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.jin.subwayalarm.domain.usecase.AdvanceJourneyUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 알람 알림의 "해제" 액션 버튼 처리. 앱 프로세스가 죽어있어도 동작해야 하므로
 * BroadcastReceiver 로 분리(NotificationHelper.ACTION_DISMISS_ALARM).
 * 실제 처리는 [AdvanceJourneyUseCase] 하나로 위임 — 다음 구간이 있으면 이어서 추적을
 * 시작하고, 마지막 구간이었으면 여정을 종료한다.
 */
@AndroidEntryPoint
class AlarmDismissReceiver : BroadcastReceiver() {

    @Inject lateinit var advanceJourney: AdvanceJourneyUseCase

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != NotificationHelper.ACTION_DISMISS_ALARM) return

        // onReceive 는 동기 콜백이라 goAsync() 로 프로세스가 조기 종료되지 않도록 보장.
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 예외가 새어나가면 미처리 코루틴 예외로 앱이 죽으므로 흡수한다(알림 해제는 best-effort).
                runCatching { advanceJourney() }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
